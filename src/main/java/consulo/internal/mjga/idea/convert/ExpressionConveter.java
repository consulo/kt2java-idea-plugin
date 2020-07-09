package consulo.internal.mjga.idea.convert;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.expression.*;
import consulo.internal.mjga.idea.convert.generate.KtToJavaClassBinder;
import consulo.internal.mjga.idea.convert.library.FunctionRemapper;
import consulo.internal.mjga.idea.convert.statement.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement;
import org.jetbrains.kotlin.resolve.source.PsiSourceFile;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.*;

/**
 * @author VISTALL
 * @since 2020-06-24
 */
public class ExpressionConveter extends KtVisitorVoid
{
	private final ConvertContext myContext;

	@NotNull
	public static GeneratedElement convertNonnull(@Nullable PsiElement element, @NotNull ConvertContext context)
	{
		if(element == null)
		{
			return new ConstantExpression("\"unsupported\"");
		}
		ExpressionConveter conveter = new ExpressionConveter(context);
		element.accept(conveter);
		GeneratedElement generatedElement = conveter.myGeneratedElement;
		if(generatedElement == null)
		{
			generatedElement = new ConstantExpression("\"unsupported '" + element.getText() + "' expression\"");
		}
		return generatedElement;
	}

	private GeneratedElement myGeneratedElement;

	public ExpressionConveter(ConvertContext context)
	{
		myContext = context;
	}

	@NotNull
	private GeneratedElement convertNonnull(@Nullable PsiElement element)
	{
		return convertNonnull(element, myContext);
	}

	@Override
	public void visitElement(@NotNull PsiElement element)
	{
		super.visitElement(element);
	}

	@Override
	public void visitSimpleNameExpression(KtSimpleNameExpression expression)
	{
		if(expression instanceof KtNameReferenceExpression)
		{
			String referencedName = expression.getReferencedName();

			myGeneratedElement = new ReferenceExpression(referencedName);

			BindingContext context = ResolutionUtils.analyze(expression);

			DeclarationDescriptor receiverResult = context.get(BindingContext.REFERENCE_TARGET, expression);
			if(receiverResult == null)
			{
				return;
			}

			@Nullable TypeName typeName = TypeConverter.convertKotlinDescriptor(receiverResult, false);
			if(typeName != null)
			{
				myGeneratedElement = new TypeReferenceExpression(typeName);
			}

			if(receiverResult instanceof PropertyDescriptor)
			{
				PropertyGetterDescriptor getter = ((PropertyDescriptor) receiverResult).getGetter();
				if(getter != null && ((PropertyDescriptor) receiverResult).getVisibility() != Visibilities.PRIVATE)
				{
					String methodName = "get" + StringUtil.capitalize(receiverResult.getName().asString());
					myGeneratedElement = new MethodCallExpression(new ReferenceExpression(methodName), Collections.emptyList());
				}

				if(receiverResult.getContainingDeclaration() instanceof PackageFragmentDescriptor)
				{
					SourceFile containingFile = ((PropertyDescriptor) receiverResult).getSource().getContainingFile();

					PsiFile file = containingFile instanceof PsiSourceFile ? ((PsiSourceFile) containingFile).getPsiFile() : null;

					if(file instanceof KtFile)
					{
						@NotNull KtToJavaClassBinder classBinder = myContext.bind((KtFile) file);

						ClassName name = ClassName.get(classBinder.getPackageName(), classBinder.getClassName());

						myGeneratedElement = new StaticTypeQualifiedExpression(name, myGeneratedElement);
					}
				}
			}
		}
	}

	@Override
	public void visitPostfixExpression(KtPostfixExpression expression)
	{
		KtExpression baseExpression = expression.getBaseExpression();

		@NotNull GeneratedElement generatedElement = convertNonnull(baseExpression);

		IElementType operationToken = expression.getOperationToken();

		// assertion - just ignore
		if(operationToken == KtTokens.EXCLEXCL)
		{
			myGeneratedElement = generatedElement;
		}
	}

	// TODO better handle safe access
	@Override
	public void visitSafeQualifiedExpression(KtSafeQualifiedExpression expression)
	{
		@NotNull GeneratedElement left = convertNonnull(expression.getReceiverExpression());
		@NotNull GeneratedElement right = convertNonnull(expression.getSelectorExpression());

		myGeneratedElement = new QualifiedExpression(left, right);
	}

	@Override
	public void visitDotQualifiedExpression(KtDotQualifiedExpression expression)
	{
		KtExpression receiver = expression.getReceiverExpression();

		GeneratedElement receiverGenerate = convertNonnull(receiver);

		GeneratedElement selectorGenerate = convertNonnull(expression.getSelectorExpression());

		if(receiver instanceof KtNameReferenceExpression)
		{
			BindingContext context = ResolutionUtils.analyze(receiver);

			DeclarationDescriptor receiverResult = context.get(BindingContext.REFERENCE_TARGET, (KtNameReferenceExpression) receiver);

			if(receiverResult instanceof LazyClassDescriptor)
			{
				SourceElement source = ((LazyClassDescriptor) receiverResult).getSource();

				if(source instanceof KotlinSourceElement)
				{
					KtElement psi = ((KotlinSourceElement) source).getPsi();

					if(psi instanceof KtObjectDeclaration)
					{
						receiverGenerate = new QualifiedExpression(receiverGenerate, new ReferenceExpression("INSTANCE"));
					}
				}
			}
		}

		myGeneratedElement = new QualifiedExpression(receiverGenerate, selectorGenerate);
	}

	@Override
	public void visitProperty(KtProperty property)
	{
		KtExpression initializer = property.getInitializer();

		BindingContext context = ResolutionUtils.analyze(property);

		DeclarationDescriptor declarationDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);

		if(declarationDescriptor == null)
		{
			return;
		}

		GeneratedElement init = initializer == null ? null : convertNonnull(initializer);

		if(declarationDescriptor instanceof LocalVariableDescriptor)
		{
			KotlinType type = ((LocalVariableDescriptor) declarationDescriptor).getType();

			TypeName typeName = TypeConverter.convertKotlinType(type);

			myGeneratedElement = new LocalVariableStatement(typeName, property.getName(), init);
		}
	}

	@Override
	public void visitStringTemplateExpression(KtStringTemplateExpression expression)
	{
		List<GeneratedElement> expressions = new ArrayList<>();

		for(KtStringTemplateEntry entry : expression.getEntries())
		{
			if(entry instanceof KtStringTemplateEntryWithExpression)
			{
				expressions.add(convertNonnull(entry.getExpression()));
			}
			else
			{
				expressions.add(new ConstantExpression("\"" + entry.getText() + "\""));
			}
		}

		myGeneratedElement = new StringBuilderExpression(expressions);
	}

	@Override
	public void visitTryExpression(KtTryExpression expression)
	{
		KtBlockExpression tryBlock = expression.getTryBlock();

		@NotNull GeneratedElement tryBlockGen = convertNonnull(tryBlock);

		List<KtCatchClause> catchClauses = expression.getCatchClauses();

		List<TryCatchStatement.Catch> catches = new ArrayList<>();
		for(KtCatchClause clause : catchClauses)
		{
			KtParameter catchParameter = clause.getCatchParameter();

			BindingContext context = ResolutionUtils.analyze(catchParameter);

			ValueDescriptor d = (ValueDescriptor) context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, catchParameter);

			@NotNull GeneratedElement body = convertNonnull(clause.getCatchBody());

			catches.add(new TryCatchStatement.Catch(MemberConverter.safeName(catchParameter.getName()), TypeConverter.convertKotlinType(d.getType()), body));
		}

		myGeneratedElement = new TryCatchStatement(tryBlockGen, catches);
	}

	@Override
	public void visitThisExpression(KtThisExpression expression)
	{
		myGeneratedElement = new ThisExpression();
	}

	@Override
	public void visitCallExpression(KtCallExpression expression)
	{
		GeneratedElement genCall = convertNonnull(expression.getCalleeExpression());

		ResolvedCall<? extends CallableDescriptor> call = ResolutionUtils.resolveToCall(expression, BodyResolveMode.FULL);

		if(call == null)
		{
			return;
		}

		CallableDescriptor resultingDescriptor = call.getResultingDescriptor();
		if(resultingDescriptor instanceof SamConstructorDescriptor)
		{
			List<? extends LambdaArgument> functionLiteralArguments = call.getCall().getFunctionLiteralArguments();

			if(functionLiteralArguments.isEmpty())
			{
				return;
			}

			LambdaArgument lambdaArgument = functionLiteralArguments.get(0);

			KtLambdaExpression lambdaExpression = lambdaArgument.getLambdaExpression();

			KtBlockExpression bodyExpression = lambdaExpression.getBodyExpression();

			myGeneratedElement = new LambdaExpression(Collections.emptyList(), convertNonnull(bodyExpression));
		}
		else if(resultingDescriptor instanceof ClassConstructorDescriptor || resultingDescriptor instanceof TypeAliasConstructorDescriptor)
		{
			List<GeneratedElement> args = new ArrayList<>();

			List<ResolvedValueArgument> valueArgumentsByIndex = call.getValueArgumentsByIndex();

			for(ResolvedValueArgument valueArgument : valueArgumentsByIndex)
			{
				for(ValueArgument argument : valueArgument.getArguments())
				{
					args.add(convertNonnull(argument.getArgumentExpression()));
				}
			}

			KotlinType returnType = resultingDescriptor.getReturnType();

			TypeName typeName = TypeConverter.convertKotlinType(returnType);

			myGeneratedElement = new NewExpression(typeName, args);
		}
		else
		{
			List<GeneratedElement> args = new ArrayList<>();

			List<ResolvedValueArgument> valueArgumentsByIndex = call.getValueArgumentsByIndex();

			for(ResolvedValueArgument valueArgument : valueArgumentsByIndex)
			{
				for(ValueArgument argument : valueArgument.getArguments())
				{
					args.add(convertNonnull(argument.getArgumentExpression()));
				}
			}

			genCall = FunctionRemapper.remap(call, genCall);
			myGeneratedElement = new MethodCallExpression(genCall, args);
		}
	}

	@Override
	public void visitBinaryExpression(KtBinaryExpression expression)
	{
		KtExpression leftExpr = expression.getLeft();

		GeneratedElement leftGen = convertNonnull(leftExpr);
		GeneratedElement rightGen = convertNonnull(expression.getRight());

		IElementType operationToken = expression.getOperationToken();
		if(operationToken == KtTokens.EQEQ)
		{
			myGeneratedElement = new MethodCallExpression(new StaticTypeQualifiedExpression(TypeName.get(Objects.class), "equals"), Arrays.asList(leftGen, rightGen));
			return;
		}

		if(operationToken == KtTokens.EXCLEQ)
		{
			myGeneratedElement = new PrefixExpression("!", new MethodCallExpression(new StaticTypeQualifiedExpression(TypeName.get(Objects.class), "equals"), Arrays.asList(leftGen, rightGen)));
			return;
		}

		if(operationToken == KtTokens.EQEQEQ)
		{
			myGeneratedElement = new BinaryExpression(leftGen, rightGen, "==");
			return;
		}

		if(operationToken == KtTokens.EXCLEQEQEQ)
		{
			myGeneratedElement = new BinaryExpression(leftGen, rightGen, "!=");
			return;
		}

		if(operationToken == KtTokens.ELVIS)
		{
			BinaryExpression condition = new BinaryExpression(leftGen, new ConstantExpression("null"), "==");
			myGeneratedElement = new TernaryExpression(condition, rightGen, leftGen);
			return;
		}

		ResolvedCall<? extends CallableDescriptor> call = ResolutionUtils.resolveToCall(expression, BodyResolveMode.FULL);

		if(call == null)
		{
			if(leftExpr instanceof KtReferenceExpression)
			{
				BindingContext context = ResolutionUtils.analyze(leftExpr);

				DeclarationDescriptor leftResult = context.get(BindingContext.REFERENCE_TARGET, (KtReferenceExpression) leftExpr);

				if(leftResult instanceof PropertyDescriptor)
				{
					PropertySetterDescriptor setter = ((PropertyDescriptor) leftResult).getSetter();

					if(setter == null || ((PropertyDescriptor) leftResult).getVisibility() == Visibilities.PRIVATE)
					{
						myGeneratedElement = new AssignExpression(convertNonnull(leftExpr), rightGen);
					}
					else
					{
						String methodName = "set" + StringUtil.capitalize(leftResult.getName().asString());
						myGeneratedElement = new MethodCallExpression(new ReferenceExpression(methodName), Arrays.asList(rightGen));
					}
				}
			}
		}

		if(myGeneratedElement != null)
		{
			return;
		}

		if(operationToken == KtTokens.EQ)
		{
			myGeneratedElement = new AssignExpression(leftGen, rightGen);
			return;
		}
	}

	@Override
	public void visitIfExpression(KtIfExpression expression)
	{
		GeneratedElement condition = convertNonnull(expression.getCondition());

		GeneratedElement trueBlock = convertNonnull(expression.getThen());

		KtExpression anElse = expression.getElse();

		GeneratedElement falseBlock = anElse == null ? null : convertNonnull(anElse);

		boolean canByTernary = false;

		PsiElement parent = expression.getParent();
		if(parent instanceof KtProperty || parent instanceof KtBinaryExpression || parent instanceof KtValueArgument)
		{
			canByTernary = true;
		}

		if(canByTernary)
		{
			myGeneratedElement = new TernaryExpression(condition, trueBlock, falseBlock);
		}
		else
		{
			myGeneratedElement = new IfStatement(condition, trueBlock, falseBlock);
		}
	}

	@Override
	public void visitConstantExpression(@NotNull KtConstantExpression expression)
	{
		myGeneratedElement = new ConstantExpression(expression.getText());
	}

	@Override
	public void visitReturnExpression(KtReturnExpression expression)
	{
		KtExpression returnedExpression = expression.getReturnedExpression();
		myGeneratedElement = returnedExpression == null ? new ReturnStatement(null) : new ReturnStatement(convertNonnull(returnedExpression));
	}

	@Override
	public void visitBlockExpression(KtBlockExpression expression)
	{
		List<KtExpression> statements = expression.getStatements();

		List<GeneratedElement> generatedElements = new ArrayList<>();
		for(KtExpression statement : statements)
		{
			GeneratedElement generatedElement = convertNonnull(statement);

			generatedElements.add(generatedElement);
		}

		myGeneratedElement = new BlockStatement(generatedElements);
	}
}

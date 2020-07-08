package consulo.internal.mjga.idea.convert;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.expression.*;
import consulo.internal.mjga.idea.convert.library.FunctionRemapper;
import consulo.internal.mjga.idea.convert.statement.BlockStatement;
import consulo.internal.mjga.idea.convert.statement.IfStatement;
import consulo.internal.mjga.idea.convert.statement.LocalVariableStatement;
import consulo.internal.mjga.idea.convert.statement.ReturnStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
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
import org.jetbrains.kotlin.types.KotlinType;

import java.util.*;

/**
 * @author VISTALL
 * @since 2020-06-24
 */
public class KtExpressionConveter extends KtVisitorVoid
{
	@Nullable
	public static GeneratedElement convert(@NotNull PsiElement element)
	{
		KtExpressionConveter conveter = new KtExpressionConveter();
		element.accept(conveter);
		GeneratedElement generatedElement = conveter.myGeneratedElement;
		return generatedElement;
	}

	@NotNull
	public static GeneratedElement convertNonnull(@Nullable PsiElement element)
	{
		if(element == null)
		{
			return new ConstantExpression("\"unsupported\"");
		}
		KtExpressionConveter conveter = new KtExpressionConveter();
		element.accept(conveter);
		GeneratedElement generatedElement = conveter.myGeneratedElement;
		if(generatedElement == null)
		{
			generatedElement = new ConstantExpression("\"unsupported\"");
		}
		return generatedElement;
	}

	private GeneratedElement myGeneratedElement;

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
		}
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

		GeneratedElement rightGen = convertNonnull(expression.getRight());

		ResolvedCall<? extends CallableDescriptor> call = ResolutionUtils.resolveToCall(expression, BodyResolveMode.FULL);

		if(call == null)
		{
			if(leftExpr instanceof KtReferenceExpression)
			{
				BindingContext context = ResolutionUtils.analyze(leftExpr);

				DeclarationDescriptor leftResult = context.get(BindingContext.REFERENCE_TARGET, (KtReferenceExpression) leftExpr);

				if(leftResult instanceof PropertyDescriptor)
				{
					if(((PropertyDescriptor) leftResult).getVisibility() == Visibilities.PRIVATE)
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
		else
		{
			GeneratedElement leftGen = convertNonnull(leftExpr);

			IElementType operationToken = expression.getOperationToken();
			if(operationToken == KtTokens.EQEQ)
			{
				myGeneratedElement = new MethodCallExpression(new StaticTypeReferenceExpression(TypeName.get(Objects.class), "equals"), Arrays.asList(leftGen, rightGen));
				return;
			}

			if(operationToken == KtTokens.EXCLEQ)
			{
				myGeneratedElement = new PrefixExpression("!", new MethodCallExpression(new StaticTypeReferenceExpression(TypeName.get(Objects.class), "equals"), Arrays.asList(leftGen, rightGen)));
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
		}
	}

	@Override
	public void visitIfExpression(KtIfExpression expression)
	{
		GeneratedElement condition = convertNonnull(expression.getCondition());

		GeneratedElement trueBlock = convertNonnull(expression.getThen());

		KtExpression anElse = expression.getElse();

		GeneratedElement falseBlock = anElse == null ? null : convert(anElse);

		boolean canByTernary = false;

		PsiElement parent = expression.getParent();
		if(parent instanceof KtProperty || parent instanceof KtBinaryExpression)
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
		myGeneratedElement = returnedExpression == null ? new ReturnStatement(null) : new ReturnStatement(convert(returnedExpression));
	}

	@Override
	public void visitBlockExpression(KtBlockExpression expression)
	{
		List<KtExpression> statements = expression.getStatements();

		List<GeneratedElement> generatedElements = new ArrayList<>();
		for(KtExpression statement : statements)
		{
			GeneratedElement generatedElement = convert(statement);

			if(generatedElement != null)
			{
				generatedElements.add(generatedElement);
			}
		}

		myGeneratedElement = new BlockStatement(generatedElements);
	}
}

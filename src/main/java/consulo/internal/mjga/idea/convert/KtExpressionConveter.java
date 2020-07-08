package consulo.internal.mjga.idea.convert;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.expression.*;
import consulo.internal.mjga.idea.convert.statement.BlockStatement;
import consulo.internal.mjga.idea.convert.statement.IfStatement;
import consulo.internal.mjga.idea.convert.statement.ReturnStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-06-24
 */
public class KtExpressionConveter extends KtVisitorVoid
{
	public static GeneratedElement convert(@NotNull BindingContext context, @NotNull PsiElement element)
	{
		KtExpressionConveter conveter = new KtExpressionConveter(context);
		element.accept(conveter);
		return conveter.myGeneratedElement;
	}

	@NotNull
	public static GeneratedElement convertNonnull(@NotNull BindingContext context, @Nullable PsiElement element)
	{
		if(element == null)
		{
			return new ConstantExpression("\"unsupported\"");
		}
		KtExpressionConveter conveter = new KtExpressionConveter(context);
		element.accept(conveter);
		GeneratedElement generatedElement = conveter.myGeneratedElement;
		if(generatedElement == null)
		{
			return new ConstantExpression("\"unsupported\"");
		}
		return generatedElement;
	}

	private final BindingContext myContext;

	public KtExpressionConveter(BindingContext context)
	{
		myContext = context;
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
	public void visitCallExpression(KtCallExpression expression)
	{
		GeneratedElement genCall = convertNonnull(myContext, expression.getCalleeExpression());

		ResolvedCall<? extends CallableDescriptor> call = ResolutionUtils.resolveToCall(expression, BodyResolveMode.FULL);

		if(call == null)
		{
			return;
		}

		List<GeneratedElement> args = new ArrayList<>();

		List<ResolvedValueArgument> valueArgumentsByIndex = call.getValueArgumentsByIndex();

		for(ResolvedValueArgument valueArgument : valueArgumentsByIndex)
		{
			for(ValueArgument argument : valueArgument.getArguments())
			{
				args.add(convertNonnull(myContext, argument.getArgumentExpression()));
			}
		}

		myGeneratedElement = new MethodCallExpression(genCall, args);
	}

	@Override
	public void visitBinaryExpression(KtBinaryExpression expression)
	{
		GeneratedElement left = convertNonnull(myContext, expression.getLeft());
		GeneratedElement right = convertNonnull(myContext, expression.getRight());

		ResolvedCall<? extends CallableDescriptor> call = ResolutionUtils.resolveToCall(expression, BodyResolveMode.FULL);

		if(call == null)
		{
			return;
		}

		IElementType operationToken = expression.getOperationToken();
		if(operationToken == KtTokens.EQEQ)
		{
			myGeneratedElement = new MethodCallExpression(new StaticTypeReference(TypeName.get(Objects.class), "equals"), Arrays.asList(left, right));
			return;
		}

		if(operationToken == KtTokens.EXCLEQ)
		{
			myGeneratedElement = new PrefixExpression("!", new MethodCallExpression(new StaticTypeReference(TypeName.get(Objects.class), "equals"), Arrays.asList(left, right)));
			return;
		}

		if(operationToken == KtTokens.EQEQEQ)
		{
			myGeneratedElement = new BinaryExpression(left, right, "==");
			return;
		}

		if(operationToken == KtTokens.EXCLEQEQEQ)
		{
			myGeneratedElement = new BinaryExpression(left, right, "!=");
			return;
		}


		CallableDescriptor resultingDescriptor = call.getResultingDescriptor();

		expression.getOperationReference();
	}

	@Override
	public void visitIfExpression(KtIfExpression expression)
	{
		GeneratedElement condition = convertNonnull(myContext, expression.getCondition());

		GeneratedElement trueBlock = convertNonnull(myContext, expression.getThen());

		KtExpression anElse = expression.getElse();

		GeneratedElement falseBlock = anElse == null ? null : convert(myContext, anElse);

		myGeneratedElement = new IfStatement(condition, trueBlock, falseBlock);
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
		myGeneratedElement = returnedExpression == null ? new ReturnStatement(null) : new ReturnStatement(convert(myContext, returnedExpression));
	}

	@Override
	public void visitBlockExpression(KtBlockExpression expression)
	{
		List<KtExpression> statements = expression.getStatements();

		List<GeneratedElement> generatedElements = new ArrayList<>();
		for(KtExpression statement : statements)
		{
			GeneratedElement generatedElement = convert(myContext, statement);

			if(generatedElement != null)
			{
				generatedElements.add(generatedElement);
			}
		}

		myGeneratedElement = new BlockStatement(generatedElements);
	}
}

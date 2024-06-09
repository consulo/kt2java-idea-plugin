package consulo.internal.mjga.idea.convert.kotlinExp;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.tree.IElementType;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.BitExpressionHelper;
import consulo.internal.mjga.idea.convert.ConvertContext;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.expression.*;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class KtBinaryExpressionAnalyzer extends ExpressionAnalyzer<KtBinaryExpression>
{
	@Override
	public GeneratedElement analyze(KtBinaryExpression expression, ConvertContext context)
	{
		KtExpression leftExpr = expression.getLeft();
		KtExpression rightExpr = expression.getRight();

		GeneratedElement leftGen = context.convertExpression(leftExpr);
		GeneratedElement rightGen = context.convertExpression(rightExpr);

		IElementType operationToken = expression.getOperationToken();
		if (operationToken == KtTokens.EQEQ)
		{
			if (ConstantExpression.isNull(rightGen))
			{
				return new BinaryExpression(leftGen, rightGen, "==");
			}
			else
			{
				return new MethodCallExpression(new StaticTypeQualifiedExpression(TypeName.get(Objects.class), "equals"), Arrays.asList(leftGen, rightGen));
			}
		}

		if (operationToken == KtTokens.EXCLEQ)
		{
			if (ConstantExpression.isNull(rightGen))
			{
				return new BinaryExpression(leftGen, rightGen, "!=");
			}
			else
			{
				return new PrefixExpression("!", new MethodCallExpression(new StaticTypeQualifiedExpression(TypeName.get(Objects.class), "equals"), Arrays.asList(leftGen, rightGen)));
			}
		}

		if (operationToken == KtTokens.EQEQEQ)
		{
			return new BinaryExpression(leftGen, rightGen, "==");
		}

		if (operationToken == KtTokens.OROR)
		{
			return new BinaryExpression(leftGen, rightGen, "||");
		}

		if (operationToken == KtTokens.ANDAND)
		{
			return new BinaryExpression(leftGen, rightGen, "&&");
		}

		if (operationToken == KtTokens.EXCLEQEQEQ)
		{
			return new BinaryExpression(leftGen, rightGen, "!=");
		}

		if (operationToken == KtTokens.ELVIS)
		{
			BinaryExpression condition = new BinaryExpression(leftGen, new ConstantExpression("null"), "==");
			return new TernaryExpression(condition, rightGen, leftGen);
		}

		ResolvedCall<? extends CallableDescriptor> call = ResolutionUtils.resolveToCall(expression, BodyResolveMode.FULL);

		GeneratedElement result = null;
		if (call == null && leftExpr != null)
		{
			ResolvedCall<? extends CallableDescriptor> leftCall = ResolutionUtils.resolveToCall(leftExpr, BodyResolveMode.FULL);

			DeclarationDescriptor leftResult = leftCall == null ? null : leftCall.getCandidateDescriptor();

			if (leftResult instanceof PropertyDescriptor)
			{
				PropertySetterDescriptor setter = ((PropertyDescriptor) leftResult).getSetter();

				if (setter == null || ((PropertyDescriptor) leftResult).getVisibility() == DescriptorVisibilities.PRIVATE && !((PropertyDescriptor) leftResult).isConst())
				{
					result = new AssignExpression(context.convertExpression(leftExpr), rightGen);
				}
				else
				{
					String setMethodName = "set" + StringUtil.capitalize(leftResult.getName().asString());
					if (leftResult instanceof SyntheticJavaPropertyDescriptor)
					{
						FunctionDescriptor setMethod = ((SyntheticJavaPropertyDescriptor) leftResult).getSetMethod();

						if (setMethod != null)
						{
							setMethodName = setMethod.getName().asString();
						}
					}


					MethodCallExpression callExpr = new MethodCallExpression(new ReferenceExpression(setMethodName), Arrays.asList(rightGen));

					if (leftGen instanceof QualifiedExpression)
					{
						GeneratedElement left = ((QualifiedExpression) leftGen).getLeft();

						result = new QualifiedExpression(left, callExpr);
					}
					else
					{
						result = callExpr;
					}
				}
			}
		}
		else if (call != null)
		{
			CallableDescriptor descriptor = call.getCandidateDescriptor();
			String bitOperator = BitExpressionHelper.remapToBitExpression(descriptor);
			if (bitOperator != null)
			{
				result = new BinaryExpression(leftGen, rightGen, bitOperator);
			}
			else if (operationToken == KtTokens.IN_KEYWORD)
			{
				// collection.contains(!)
				QualifiedExpression ref = new QualifiedExpression(rightGen, new ReferenceExpression(descriptor.getName().toString()));
				result = new MethodCallExpression(ref, List.of(leftGen));
			}
			else
			{
				QualifiedExpression ref = new QualifiedExpression(leftGen, new ReferenceExpression(descriptor.getName().toString()));
				result = new MethodCallExpression(ref, List.of(rightGen));
			}
		}

		if (result != null)
		{
			return result;
		}

		if (operationToken == KtTokens.EQ)
		{
			return new AssignExpression(leftGen, rightGen);
		}

		String text = expression.getOperationReference().getText();

		return new BinaryExpression(leftGen, rightGen, text);
	}
}

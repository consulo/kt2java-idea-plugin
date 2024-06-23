package consulo.internal.mjga.idea.convert.kotlinExp;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.ConvertContext;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.expression.LambdaExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 23-Jun-24
 */
public class KtLambdaExpressionAnalyzer extends KtExpressionAnalyzer<KtLambdaExpression>
{
	@Override
	public GeneratedElement analyze(KtLambdaExpression lambdaExpression, ConvertContext convertContext)
	{
		List<KtParameter> valueParameters = lambdaExpression.getValueParameters();

		BindingContext context = ResolutionUtils.analyze(lambdaExpression);

		List<Pair<TypeName, String>> params = new ArrayList<>();
		for(KtParameter valueParameter : valueParameters)
		{
			params.add(Pair.create(null, valueParameter.getName()));
		}

		KtBlockExpression bodyExpression = lambdaExpression.getBodyExpression();

		if(valueParameters.isEmpty())
		{
			ValueParameterDescriptor[] foundIt = new ValueParameterDescriptor[1];
			bodyExpression.accept(new PsiRecursiveElementWalkingVisitor()
			{
				@Override
				public void visitElement(PsiElement element)
				{
					if(element instanceof KtSimpleNameExpression)
					{
						DeclarationDescriptor declarationDescriptor = context.get(BindingContext.REFERENCE_TARGET, (KtSimpleNameExpression) element);
						if(declarationDescriptor instanceof ValueParameterDescriptor)
						{
							if(context.get(BindingContext.AUTO_CREATED_IT, (ValueParameterDescriptor) declarationDescriptor) == true)
							{
								foundIt[0] = (ValueParameterDescriptor) declarationDescriptor;
								stopWalking();
							}
						}
					}
					else if(element instanceof KtLambdaExpression)
					{
						stopWalking();
					}
					else
					{
						super.visitElement(element);
					}
				}
			});

			if(foundIt[0] != null)
			{
				params.add(Pair.create(null, foundIt[0].getName().toString()));
			}
		}

		@NotNull GeneratedElement body = convertContext.convertExpression(bodyExpression);

		return new LambdaExpression(params, body);
	}
}

package consulo.internal.mjga.idea.convert.kotlinExp;

import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.BitExpressionHelper;
import consulo.internal.mjga.idea.convert.ConvertContext;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.expression.*;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement;
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;

public class KtDotQualifiedExpressionAnalyzer extends KtExpressionAnalyzer<KtDotQualifiedExpression>
{
	@Override
	public GeneratedElement analyze(KtDotQualifiedExpression expression, ConvertContext context)
	{
		BindingContext bindingContext = ResolutionUtils.analyze(expression);

		KtExpression receiver = expression.getReceiverExpression();
		KtExpression selector = expression.getSelectorExpression();

		GeneratedElement receiverGenerate = context.convertExpression(receiver);

		GeneratedElement selectorGenerate = context.convertExpression(selector);

		if (receiver instanceof KtNameReferenceExpression)
		{
			DeclarationDescriptor receiverResult = bindingContext.get(BindingContext.REFERENCE_TARGET, (KtNameReferenceExpression) receiver);

			if (receiverResult instanceof LazyClassDescriptor)
			{
				SourceElement source = ((LazyClassDescriptor) receiverResult).getSource();

				if (source instanceof KotlinSourceElement)
				{
					KtElement psi = ((KotlinSourceElement) source).getPsi();

					if (psi instanceof KtObjectDeclaration)
					{
						receiverGenerate = new QualifiedExpression(receiverGenerate, new ReferenceExpression("INSTANCE"));
					}
				}
			}
		}

		ResolvedCall<? extends CallableDescriptor> call = ResolutionUtils.resolveToCall(selector, BodyResolveMode.FULL);

		GeneratedElement result = new QualifiedExpression(receiverGenerate, selectorGenerate);

		// if qualified expression is new expression - not interest in it, due selector will generate correct qualifier
		if (call != null && call.getCandidateDescriptor() instanceof ConstructorDescriptor)
		{
			return selectorGenerate;
		}

		String bitToken = null;
		if (call != null)
		{
			bitToken = BitExpressionHelper.remapToBitExpression(call.getCandidateDescriptor());
		}

		if (bitToken != null)
		{
			result = new PrefixExpression(bitToken, receiverGenerate);
		}
		else
		{
			// extension call
			if (call != null && call.getExtensionReceiver() != null)
			{
				GeneratedElement targetSelector = selectorGenerate;
				TypeName qualifiedType = null;

				if (targetSelector instanceof StaticTypeQualifiedExpression)
				{
					qualifiedType = ((StaticTypeQualifiedExpression) targetSelector).getTypeName();
					targetSelector = ((StaticTypeQualifiedExpression) targetSelector).getSelector();
				}

				CallableDescriptor candidateDescriptor = call.getCandidateDescriptor();
				// ignore syntetic java properties, they mapped as extension
				if (!(candidateDescriptor instanceof SyntheticJavaPropertyDescriptor))
				{
					if (targetSelector instanceof MethodCallExpression)
					{
						GeneratedElement oldCall = ((MethodCallExpression) targetSelector).getCall();
						List<GeneratedElement> oldArguments = ((MethodCallExpression) targetSelector).getArguments();

						ArrayList<GeneratedElement> newArgs = new ArrayList<>(oldArguments);
						newArgs.add(0, receiverGenerate);

						MethodCallExpression newCall = new MethodCallExpression(oldCall, newArgs);
						result = qualifiedType == null ? newCall : new StaticTypeQualifiedExpression(qualifiedType, newCall);
					}
				}
			}
		}

		return result;
	}
}

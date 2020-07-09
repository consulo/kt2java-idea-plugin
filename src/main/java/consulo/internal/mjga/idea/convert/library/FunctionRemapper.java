package consulo.internal.mjga.idea.convert.library;

import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.expression.StaticTypeQualifiedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class FunctionRemapper
{
	@NotNull
	public static GeneratedElement remap(ResolvedCall<? extends CallableDescriptor> call, GeneratedElement genCall)
	{
		CallableDescriptor resultingDescriptor = call.getResultingDescriptor();

		DeclarationDescriptor containingDeclaration = resultingDescriptor.getContainingDeclaration();

		if(containingDeclaration instanceof PackageFragmentDescriptor)
		{
			FqName fqName = ((PackageFragmentDescriptor) containingDeclaration).getFqName();

			if(fqName.equals(FqName.fromSegments(Arrays.asList("kotlin", "io"))) && resultingDescriptor.getName().asString().equals("println"))
			{
				return new StaticTypeQualifiedExpression(TypeName.get(System.class), "out.println");
			}
		}
		return genCall;
	}
}

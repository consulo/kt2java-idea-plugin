package consulo.internal.mjga.idea.convert;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-07-15
 */
public class BitExpressionHelper
{
	private static final Map<String, String> ourBitFuncNames = new HashMap<>();

	static
	{
		ourBitFuncNames.put("and", "&");
		ourBitFuncNames.put("or", "|");
		ourBitFuncNames.put("xor", "^");
		ourBitFuncNames.put("shl", "<<");
		ourBitFuncNames.put("shr", ">>");
		ourBitFuncNames.put("shr", ">>");
		ourBitFuncNames.put("ushr", ">>>");
		ourBitFuncNames.put("inv", "~");
		ourBitFuncNames.put("plus", "+");
		ourBitFuncNames.put("minus", "-");
		ourBitFuncNames.put("div", "/");
		ourBitFuncNames.put("mul", "*");
	}

	private static final Set<String> ourPrimitives = ContainerUtil.newHashSet("kotlin.Int");

	@Nullable
	public static String remapToBitExpression(@NotNull CallableDescriptor callableDescriptor)
	{
		String name = callableDescriptor.getName().asString();

		String operator = ourBitFuncNames.get(name);

		if(operator == null)
		{
			return null;
		}

		DeclarationDescriptor containingDeclaration = callableDescriptor.getContainingDeclaration();

		if(containingDeclaration instanceof ClassDescriptor)
		{
			FqNameUnsafe fqName = DescriptorUtils.getFqName(containingDeclaration);

			return ourPrimitives.contains(fqName.asString()) ? operator : null;
		}

		return null;
	}
}

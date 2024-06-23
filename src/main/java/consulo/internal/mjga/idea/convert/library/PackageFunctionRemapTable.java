package consulo.internal.mjga.idea.convert.library;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.TypeConverter;
import consulo.internal.mjga.idea.convert.expression.*;
import consulo.internal.mjga.idea.convert.statement.SynchronizedStatement;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public enum PackageFunctionRemapTable
{
	kotlin__arrayOf()
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					Map<TypeParameterDescriptor, KotlinType> typeArguments = resolvedCall.getTypeArguments();

					KotlinType first = ContainerUtil.getFirstItem(typeArguments.values());

					ArrayTypeName arrayType = ArrayTypeName.of(TypeConverter.convertKotlinType(first));

					return new NewArrayExpression(arrayType, args);
				}
			},
	kotlin__byteArrayOf
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					return new NewArrayExpression(ArrayTypeName.of(byte.class), args);
				}
			},
	kotlin__shortArrayOf
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					return new NewArrayExpression(ArrayTypeName.of(short.class), args);
				}
			},
	kotlin__intArrayOf
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					return new NewArrayExpression(ArrayTypeName.of(int.class), args);
				}
			},
	kotlin__longArrayOf
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					return new NewArrayExpression(ArrayTypeName.of(long.class), args);
				}
			},
	kotlin__floatArrayOf
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					return new NewArrayExpression(ArrayTypeName.of(float.class), args);
				}
			},
	kotlin__doubleArrayOf
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					return new NewArrayExpression(ArrayTypeName.of(double.class), args);
				}
			},
	kotlin_io__println()
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					return new MethodCallExpression(new StaticTypeQualifiedExpression(TypeName.get(System.class), "out.println"), args);
				}

				@Override
				public boolean modifyCallIfPackageOwner()
				{
					return true;
				}
			},
	kotlin_collections__mutableSetOf()
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					Map<TypeParameterDescriptor, KotlinType> typeArguments = resolvedCall.getTypeArguments();
					KotlinType first = ContainerUtil.getFirstItem(typeArguments.values());
					TypeName typeArg = TypeConverter.convertKotlinType(first);
					return new NewExpression(TypeName.get(HashSet.class), List.of(typeArg), args);
				}
			},
	kotlin__synchronized()
			{
				@Override
				public GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args)
				{
					LambdaExpression lambdaExpression = (LambdaExpression) args.get(1);
					GeneratedElement data = lambdaExpression.getBlock();
					return new SynchronizedStatement(args.get(0), data);
				}
			},
	;

	private final FqName myPackageName;
	private final Name myFunctionName;

	PackageFunctionRemapTable()
	{
		List<String> parts = StringUtil.split(name(), "__");

		myPackageName = FqName.fromSegments(StringUtil.split(parts.get(0), "_"));
		myFunctionName = Name.identifier(parts.get(1));

		PackageFunctionRemapTables.table.put(Map.entry(myPackageName, myFunctionName), this);
	}

	public abstract GeneratedElement generate(ResolvedCall<? extends CallableDescriptor> resolvedCall, List<GeneratedElement> args);

	public FqName getPackageName()
	{
		return myPackageName;
	}

	public Name getFunctionName()
	{
		return myFunctionName;
	}

	public boolean modifyCallIfPackageOwner()
	{
		return false;
	}
}

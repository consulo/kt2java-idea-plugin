package consulo.internal.mjga.idea.convert.type;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class StdTypeRemapper implements TypeRemapper
{
	private String packageName;
	private String typeName;

	private TypeName notNullType;
	private TypeName nullableType;

	public StdTypeRemapper(String packageName, String typeName, TypeName notNullType, TypeName nullableType)
	{
		this.packageName = packageName;
		this.typeName = typeName;
		this.notNullType = notNullType;
		this.nullableType = nullableType;
	}

	@Override
	public @Nullable TypeName remap(DeclarationDescriptor declarationDescriptor, boolean nullable)
	{
		if(declarationDescriptor instanceof DeserializedClassDescriptor)
		{
			Name name = declarationDescriptor.getName();

			DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();

			if(typeName.equals(name.asString()) && containingDeclaration instanceof PackageFragmentDescriptor && ((PackageFragmentDescriptor) containingDeclaration).getFqName().equals(FqName.topLevel
					(Name.identifier(packageName))))
			{
				if(nullable)
				{
					return nullableType;
				}
				return notNullType;
			}
		}

		return null;
	}
}

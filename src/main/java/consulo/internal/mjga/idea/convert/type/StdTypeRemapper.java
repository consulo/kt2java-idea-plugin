package consulo.internal.mjga.idea.convert.type;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;

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
	public @Nullable TypeName remap(KotlinType kotlinType)
	{
		TypeConstructor constructor = kotlinType.getConstructor();

		ClassifierDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();


		if(declarationDescriptor instanceof DeserializedClassDescriptor)
		{
			Name name = declarationDescriptor.getName();

			DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();

			if(typeName.equals(name.asString()) && containingDeclaration instanceof PackageFragmentDescriptor && ((PackageFragmentDescriptor) containingDeclaration).getFqName().equals(FqName.topLevel
					(Name.identifier(packageName))))
			{
				if(kotlinType.isMarkedNullable())
				{
					return nullableType;
				}
				return notNullType;
			}
		}

		return null;
	}
}

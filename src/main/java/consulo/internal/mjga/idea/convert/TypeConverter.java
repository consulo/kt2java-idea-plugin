package consulo.internal.mjga.idea.convert;

import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class TypeConverter
{
	public static TypeName convertKotlinType(KotlinType kotlinType)
	{
		TypeConstructor constructor = kotlinType.getConstructor();

		ClassifierDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();

		if(declarationDescriptor instanceof LazyJavaClassDescriptor)
		{
			JavaClass jClass = ((LazyJavaClassDescriptor) declarationDescriptor).getJClass();

			return ClassName.bestGuess(jClass.getFqName().toString());
		}

		return ClassName.get("", "ErrorType");
	}

	@NotNull
	public static TypeName convertJavaPsiType(PsiType psiType)
	{
		if(psiType.equals(PsiType.VOID))
		{
			return TypeName.VOID;
		}

		if(psiType.equals(PsiType.INT))
		{
			return TypeName.INT;
		}

		if(psiType.equals(PsiType.SHORT))
		{
			return TypeName.SHORT;
		}

		if(psiType.equals(PsiType.BYTE))
		{
			return TypeName.BYTE;
		}

		if(psiType.equals(PsiType.FLOAT))
		{
			return TypeName.FLOAT;
		}

		if(psiType.equals(PsiType.CHAR))
		{
			return TypeName.CHAR;
		}

		if(psiType.equals(PsiType.DOUBLE))
		{
			return TypeName.DOUBLE;
		}

		if(psiType.equals(PsiType.BOOLEAN))
		{
			return TypeName.BOOLEAN;
		}

		if(psiType instanceof PsiClassType)
		{
			PsiType[] parameters = ((PsiClassType) psiType).getParameters();
			if(parameters.length > 0)
			{
				ClassName typeName = (ClassName) convertJavaPsiType(((PsiClassType) psiType).rawType());

				List<TypeName> mapArguments = Arrays.stream(parameters).map(TypeConverter::convertJavaPsiType).collect(Collectors.toList());
				return ParameterizedTypeName.get(typeName, mapArguments.toArray(new TypeName[0]));
			}
			else
			{
				return ClassName.bestGuess(psiType.getCanonicalText());
			}
		}
		else if(psiType instanceof PsiArrayType)
		{
			PsiType componentType = ((PsiArrayType) psiType).getComponentType();

			TypeName typeName = convertJavaPsiType(componentType);

			return ArrayTypeName.of(typeName);
		}
		return ClassName.get("", "unknown" + psiType.getClass().getSimpleName());
	}
}

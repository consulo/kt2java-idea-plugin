package consulo.internal.mjga.idea.convert;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.squareup.javapoet.*;
import consulo.internal.mjga.idea.convert.type.StdTypeRemapper;
import consulo.internal.mjga.idea.convert.type.TypeRemapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeProjection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class TypeConverter
{
	private static final List<TypeRemapper> ourTypeRemappers = new ArrayList<>();

	private static final Map<ClassName, TypeName> ourDefaultRemaper = new HashMap<>();

	static
	{
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Any", TypeName.OBJECT, TypeName.get(Object.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Char", TypeName.CHAR, TypeName.get(Character.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Byte", TypeName.BYTE, TypeName.get(Byte.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Short", TypeName.SHORT, TypeName.get(Short.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Int", TypeName.INT, TypeName.get(Integer.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Long", TypeName.LONG, TypeName.get(Long.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Float", TypeName.FLOAT, TypeName.get(Float.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Double", TypeName.DOUBLE, TypeName.get(Double.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "Boolean", TypeName.BOOLEAN, TypeName.get(Boolean.class)));
		ourTypeRemappers.add(new StdTypeRemapper("kotlin", "String", TypeName.get(String.class), TypeName.get(String.class)));
	}

	static
	{
		ourDefaultRemaper.put(ClassName.bestGuess("kotlin.collections.MutableSet"), ClassName.get(Set.class));
		ourDefaultRemaper.put(ClassName.bestGuess("kotlin.collections.Set"), ClassName.get(Set.class));
		ourDefaultRemaper.put(ClassName.bestGuess("kotlin.collections.List"), ClassName.get(List.class));
		ourDefaultRemaper.put(ClassName.bestGuess("kotlin.collections.MutableList"), ClassName.get(List.class));
		ourDefaultRemaper.put(ClassName.bestGuess("kotlin.ByteArray"), ArrayTypeName.of(TypeName.BYTE));
		ourDefaultRemaper.put(ClassName.bestGuess("kotlin.ShortArray"), ArrayTypeName.of(TypeName.SHORT));
		ourDefaultRemaper.put(ClassName.bestGuess("kotlin.IntArray"), ArrayTypeName.of(TypeName.INT));
	}

	@NotNull
	public static TypeName convertKotlinType(@NotNull KotlinType kotlinType)
	{
		TypeConstructor constructor = kotlinType.getConstructor();

		ClassifierDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();

		TypeName typeName = convertKotlinDescriptor(declarationDescriptor, kotlinType.getArguments(), kotlinType.isMarkedNullable());
		if(typeName == null)
		{
			return ClassName.get("", "ErrorType");
		}

		List<TypeProjection> arguments = kotlinType.getArguments();

		if(arguments.isEmpty())
		{
			return typeName;
		}

		List<TypeName> newArgs = arguments.stream().map(typeProjection -> convertKotlinType(typeProjection.getType())).toList();

		if(typeName.equals(ClassName.get("kotlin", "Array")) && newArgs.size() == 1)
		{
			return ArrayTypeName.of(newArgs.get(0));
		}

		return ParameterizedTypeName.get((ClassName) typeName, newArgs.toArray(new TypeName[0]));
	}

	@Nullable
	public static TypeName convertKotlinDescriptor(DeclarationDescriptor declarationDescriptor, boolean nullable)
	{
		return convertKotlinDescriptor(declarationDescriptor, Collections.emptyList(), nullable);
	}

	@Nullable
	public static TypeName convertKotlinDescriptor(DeclarationDescriptor declarationDescriptor, List<TypeProjection> arguments, boolean nullable)
	{
		if(declarationDescriptor instanceof LazyJavaClassDescriptor)
		{
			JavaClass jClass = ((LazyJavaClassDescriptor) declarationDescriptor).getJClass();

			return ClassName.bestGuess(jClass.getFqName().toString());
		}

		for(TypeRemapper typeRemapper : ourTypeRemappers)
		{
			@Nullable TypeName remap = typeRemapper.remap(declarationDescriptor, nullable);
			if(remap != null)
			{
				return remap;
			}
		}

		if(declarationDescriptor instanceof LazyClassDescriptor)
		{
			SourceElement source = ((DeclarationDescriptorWithSource) declarationDescriptor).getSource();

			if(source instanceof KotlinSourceElement)
			{
				KtElement psi = ((KotlinSourceElement) source).getPsi();

				if(psi instanceof KtClassOrObject)
				{
					if(psi instanceof KtObjectDeclaration && ((KtObjectDeclaration) psi).isCompanion())
					{
						KtClass ktClass = PsiTreeUtil.getParentOfType(psi, KtClass.class);
						assert ktClass != null;
						return ClassName.bestGuess(ktClass.getFqName().toString());
					}

					if(psi.getParent() instanceof KtObjectLiteralExpression)
					{
						return ExpressionConveter.calculateAnonymousType((KtObjectLiteralExpression) psi.getParent());
					}

					FqName fqName = ((KtClassOrObject) psi).getFqName();

					String qName = fqName.toString();

					return ClassName.bestGuess(qName);
				}
			}
		}

		if(declarationDescriptor instanceof ClassDescriptor)
		{
			DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();

			String packageName = "";

			if(containingDeclaration instanceof PackageFragmentDescriptor)
			{
				packageName = ((PackageFragmentDescriptor) containingDeclaration).getFqName().toString();
			}

			ClassName defaultClassName = ClassName.get(packageName, declarationDescriptor.getName().asString());

			TypeName remap = ourDefaultRemaper.get(defaultClassName);
			if(remap != null)
			{
				return remap;
			}

			return defaultClassName;
		}

		return null;
	}

	@NotNull
	public static TypeName convertJavaPsiType(PsiType psiType)
	{
		if(psiType.equals(PsiTypes.voidType()))
		{
			return TypeName.VOID;
		}

		if(psiType.equals(PsiTypes.longType()))
		{
			return TypeName.LONG;
		}

		if(psiType.equals(PsiTypes.intType()))
		{
			return TypeName.INT;
		}

		if(psiType.equals(PsiTypes.shortType()))
		{
			return TypeName.SHORT;
		}

		if(psiType.equals(PsiTypes.byteType()))
		{
			return TypeName.BYTE;
		}

		if(psiType.equals(PsiTypes.floatType()))
		{
			return TypeName.FLOAT;
		}

		if(psiType.equals(PsiTypes.charType()))
		{
			return TypeName.CHAR;
		}

		if(psiType.equals(PsiTypes.doubleType()))
		{
			return TypeName.DOUBLE;
		}

		if(psiType.equals(PsiTypes.booleanType()))
		{
			return TypeName.BOOLEAN;
		}

		if(psiType instanceof PsiClassType)
		{
			PsiType[] parameters = ((PsiClassType) psiType).getParameters();
			if(parameters.length > 0)
			{
				PsiClassType rawType = ((PsiClassType) psiType).rawType();
				ClassName typeName = null;
				if(rawType == psiType)
				{
					PsiClass target = rawType.resolve();

					typeName = ClassName.bestGuess(target.getQualifiedName());
				}
				else
				{
					typeName = (ClassName) convertJavaPsiType(rawType);
				}

				List<TypeName> mapArguments = Arrays.stream(parameters).map(TypeConverter::convertJavaPsiType).toList();
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
		else if(psiType instanceof PsiWildcardType)
		{
			boolean isExtends = ((PsiWildcardType) psiType).isExtends();
			boolean isSuper = ((PsiWildcardType) psiType).isSuper();
			PsiType bound = ((PsiWildcardType) psiType).getBound();

			@NotNull TypeName convertedBound = bound != null ? convertJavaPsiType(bound) : TypeName.OBJECT;

			if(isSuper)
			{
				return WildcardTypeName.supertypeOf(convertedBound);
			}
			else if(isExtends)
			{
				return WildcardTypeName.subtypeOf(convertedBound);
			}
		}
		return ClassName.get("", "unknown" + psiType.getClass().getSimpleName());
	}
}

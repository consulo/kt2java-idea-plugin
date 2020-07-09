package consulo.internal.mjga.idea.convert;

import com.intellij.lang.java.lexer.JavaLexer;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.squareup.javapoet.*;
import consulo.internal.mjga.idea.convert.generate.KtToJavaClassBinder;
import consulo.internal.mjga.idea.convert.statement.ReturnStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport;
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade;
import org.jetbrains.kotlin.asJava.classes.KtUltraLightMethodForSourceDeclaration;
import org.jetbrains.kotlin.asJava.elements.KtLightElement;
import org.jetbrains.kotlin.asJava.elements.KtLightFieldForSourceDeclarationSupport;
import org.jetbrains.kotlin.psi.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2018-10-15
 */
public class MemberConverter
{
	public static void run(ConvertContext context)
	{
		Collection<KtFile> files = context.getFiles();

		for(KtFile file : files)
		{
			convert(context, file);
		}
	}

	private static void convert(ConvertContext context, KtFile file)
	{
		KotlinAsJavaSupport kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(file.getProject());

		Collection<PsiClass> facadeClassesInPackage = kotlinAsJavaSupport.getFacadeClassesInPackage(file.getPackageFqName(), file.getResolveScope());

		PsiClass fileClass = null;

		for(PsiClass psiClass : facadeClassesInPackage)
		{
			if(psiClass instanceof KtLightClassForFacade)
			{
				Collection<KtFile> files = ((KtLightClassForFacade) psiClass).getFiles();
				if(files.contains(file))
				{
					fileClass = psiClass;
					break;
				}
			}
		}

		// just bind file to java class, may skipped if empty
		KtToJavaClassBinder fileClassBinder = context.bind(file);
		fileClassBinder.setJavaWrapper(fileClass);

		// bind classes
		for(KtDeclaration declaration : file.getDeclarations())
		{
			if(declaration instanceof KtClassOrObject)
			{
				PsiClass[] classes = JavaPsiFacade.getInstance(file.getProject()).findClasses(((KtClassOrObject) declaration).getFqName().toString(), file.getResolveScope());

				for(PsiClass clazz : classes)
				{
					if(clazz instanceof KtLightElement)
					{
						KtToJavaClassBinder binder = context.bind((KtClassOrObject) declaration);
						binder.setJavaWrapper(clazz);
					}
				}
			}
		}
	}

	public static void writeFiles(ConvertContext context)
	{
		context.forEach(ktToJavaClassBinder ->
		{
			try
			{
				writeFile(context, ktToJavaClassBinder);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		});
	}

	private static void writeFile(ConvertContext context, KtToJavaClassBinder binder) throws IOException
	{
		KtElement sourceElement = binder.getSourceElement();
		Module module = ReadAction.compute(() -> ModuleUtil.findModuleForPsiElement(sourceElement));
		if(module == null)
		{
			return;
		}

		ContentEntry entry = ReadAction.compute(() -> ModuleRootManager.getInstance(module).getContentEntries()[0]);
		SourceFolder sourceFolder = entry.getSourceFolders(JavaSourceRootType.SOURCE).get(0);

		VirtualFile scopeDir = sourceFolder.getFile();

		TypeSpec.Builder builder;

		boolean isInterface;
		switch(binder.getSourceClassType())
		{
			default:
				isInterface = false;
				builder = TypeSpec.classBuilder(binder.getClassName());
				break;
			case INTERFACE:
				isInterface = true;
				builder = TypeSpec.interfaceBuilder(binder.getClassName());
				break;
			case ENUM:
				// todo
				isInterface = false;
				builder = TypeSpec.classBuilder(binder.getClassName());
				break;
		}

		boolean containsAnyChild = ReadAction.compute(() -> convert(context, builder, binder.getJavaWrapper(), isInterface));

		if(!containsAnyChild)
		{
			return;
		}

		JavaFile.Builder fileBuilder = JavaFile.builder(binder.getPackageName(), builder.build());

		JavaFile javaFile = fileBuilder.build();

		String text = javaFile.toString();

		WriteAction.runAndWait(() -> {
			VirtualFile targetDirectory = scopeDir;

			if(!StringUtil.isEmpty(binder.getPackageName()))
			{
				String directories = binder.getPackageName().replace(".", "/");

				targetDirectory = VfsUtil.createDirectoryIfMissing(targetDirectory, directories);
			}

			String file = binder.getClassName() + ".java";
			VirtualFile child = targetDirectory.findChild(file);
			if(child == null)
			{
				child = targetDirectory.createChildData(null, file);
			}
			child.setBinaryContent(text.getBytes(StandardCharsets.UTF_8));
		});
	}

	private static boolean convert(ConvertContext context, TypeSpec.Builder builder, PsiClass javaWrapper, boolean isInterface)
	{
		if(!(javaWrapper instanceof PsiExtensibleClass))
		{
			return false;
		}

		boolean hasAnyChild = false;

		builder.addModifiers(convertModifiers(javaWrapper));

		KtClassOrObject ktClassOrObject = null;
		if(javaWrapper instanceof KtLightElement)
		{
			KtElement kotlinOrigin = ((KtLightElement) javaWrapper).getKotlinOrigin();

			if(kotlinOrigin instanceof KtClassOrObject)
			{
				ktClassOrObject = (KtClassOrObject) kotlinOrigin;
			}
		}

		JvmReferenceType superClass = javaWrapper.getSuperClassType();
		if(superClass != null)
		{
			builder.superclass(TypeConverter.convertJavaPsiType((PsiType) superClass));
		}

		JvmReferenceType[] interfaceTypes = javaWrapper.getInterfaceTypes();
		for(JvmReferenceType interfaceType : interfaceTypes)
		{
			builder.addSuperinterface(TypeConverter.convertJavaPsiType((PsiType) interfaceType));
		}

		ClassName thisTypeRef = ClassName.bestGuess(javaWrapper.getQualifiedName());

		for(PsiClass innerClass : javaWrapper.getInnerClasses())
		{
			if(!(innerClass instanceof PsiExtensibleClass))
			{
				continue;
			}

			if("Companion".equals(innerClass.getName()))
			{
				hasAnyChild |= mapMembers(context, thisTypeRef, (PsiExtensibleClass) innerClass, ktClassOrObject, false, (p, f) -> {
					f.addModifiers(Modifier.STATIC);
					builder.addField(f.build());
				}, (p, m) -> {
					if(p.isConstructor())
					{
						// not constructor from compation object
						return;
					}
					m.addModifiers(Modifier.STATIC);
					builder.addMethod(m.build());
				});
			}
		}

		hasAnyChild |= mapMembers(context, thisTypeRef, (PsiExtensibleClass) javaWrapper, ktClassOrObject, isInterface, (p, f) -> builder.addField(f.build()), (p, m) -> builder.addMethod(m.build()));
		return hasAnyChild;
	}

	private static boolean mapMembers(@NotNull ConvertContext context,
									  @NotNull TypeName thisTypeRef,
									  @NotNull PsiExtensibleClass javaWrapper,
									  @Nullable KtClassOrObject ktClassOrObject,
									  boolean isInterface,
									  @NotNull BiConsumer<PsiField, FieldSpec.Builder> fieldBuilders,
									  @NotNull BiConsumer<PsiMethod, MethodSpec.Builder> methodBuilders)
	{
		boolean hasAnyChild = false;

		List<PsiField> fields = javaWrapper.getOwnFields();
		for(PsiField field : fields)
		{
			// companion field
			if(field.hasModifier(JvmModifier.STATIC) && "Companion".equals(field.getName()))
			{
				continue;
			}

			hasAnyChild = true;

			FieldSpec.Builder fieldBuilder = FieldSpec.builder(TypeConverter.convertJavaPsiType(field.getType()), safeName(field.getName()), convertModifiers(field, isInterface));

			if(field instanceof KtLightFieldForSourceDeclarationSupport)
			{
				KtDeclaration kotlinOrigin = ((KtLightFieldForSourceDeclarationSupport) field).getKotlinOrigin();

				if(kotlinOrigin instanceof KtProperty)
				{
					KtExpression initializer = ((KtProperty) kotlinOrigin).getInitializer();

					if(initializer != null)
					{
						GeneratedElement codeBlock = ExpressionConveter.convertNonnull(initializer, context);
						fieldBuilder.initializer(codeBlock.generate());
					}
				}
			}

			if(ktClassOrObject instanceof KtObjectDeclaration && "INSTANCE".equals(field.getName()))
			{
				fieldBuilder.initializer(CodeBlock.of("new $T()", thisTypeRef));
			}

			fieldBuilders.accept(field, fieldBuilder);
		}

		List<PsiMethod> ownMethods = javaWrapper.getOwnMethods();
		for(PsiMethod methodOrConstructor : ownMethods)
		{
			hasAnyChild = true;

			KtValVarKeywordOwner ktPropertyOrParameter = null;
			KtExpression body = null;
			if(methodOrConstructor instanceof KtUltraLightMethodForSourceDeclaration)
			{
				LightMemberOrigin lightMemberOrigin = ((KtUltraLightMethodForSourceDeclaration) methodOrConstructor).getLightMemberOrigin();

				KtDeclaration originalElement = lightMemberOrigin == null ? null : lightMemberOrigin.getOriginalElement();

				if(originalElement instanceof KtFunction)
				{
					KtExpression bodyExpression = ((KtFunction) originalElement).getBodyExpression();

					if(bodyExpression != null)
					{
						body = bodyExpression;
					}
					else
					{
						body = ((KtFunction) originalElement).getBodyBlockExpression();
					}
				}
				else if(originalElement instanceof KtValVarKeywordOwner)
				{
					ktPropertyOrParameter = (KtValVarKeywordOwner) originalElement;
				}
			}

			boolean isConstructor = methodOrConstructor.isConstructor();

			String methodName = safeName(methodOrConstructor.getName());
			MethodSpec.Builder methodBuilder = isConstructor ? MethodSpec.constructorBuilder() : MethodSpec.methodBuilder(methodName);
			methodBuilder.addModifiers(convertModifiers(methodOrConstructor, isInterface));
			if(!isConstructor)
			{
				methodBuilder.returns(TypeConverter.convertJavaPsiType(methodOrConstructor.getReturnType()));
			}

			PsiParameter[] parameters = methodOrConstructor.getParameterList().getParameters();
			for(PsiParameter parameter : parameters)
			{
				methodBuilder.addParameter(TypeConverter.convertJavaPsiType(parameter.getType()), safeName(parameter.getName()));
			}

			if(body != null)
			{
				GeneratedElement generatedElement = ExpressionConveter.convertNonnull(body, context);
				if(body instanceof KtBlockExpression)
				{
					methodBuilder.addCode(generatedElement.generate());
				}
				else
				{
					methodBuilder.addCode(new ReturnStatement(generatedElement).wantSemicolon(true).generate());
				}
			}
			else if(ktPropertyOrParameter != null && !methodOrConstructor.hasModifier(JvmModifier.ABSTRACT))
			{
				String name = safeName(((PsiNamedElement) ktPropertyOrParameter).getName());

				if(methodName.startsWith("get") || methodName.startsWith("is"))
				{
					methodBuilder.addCode(CodeBlock.of("return $L;", name));
				}
				else
				{
					methodBuilder.addCode(CodeBlock.of("this.$L = $L;", name, name));
				}
			}
			else if(isConstructor && ktClassOrObject instanceof KtClass && !(((KtClass) ktClassOrObject).isData()))
			{
				KtPrimaryConstructor primaryConstructor = ktClassOrObject.getPrimaryConstructor();

				if(primaryConstructor != null)
				{
					for(PsiParameter parameter : parameters)
					{
						methodBuilder.addCode(CodeBlock.of("this.$L = $L;\n", parameter.getName(), parameter.getName()));
					}
				}

			}
			else if(ktClassOrObject instanceof KtClass && ((KtClass) ktClassOrObject).isData())
			{
				List<KtParameter> primaryConstructorParameters = ktClassOrObject.getPrimaryConstructorParameters();

				if("hashCode".equals(methodName) && parameters.length == 0)
				{
					List<CodeBlock> params = primaryConstructorParameters.stream().map(it -> CodeBlock.of("$L", safeName(it.getName()))).collect(Collectors.toList());

					methodBuilder.addCode(CodeBlock.of("return $T.hash($L);", Objects.class, CodeBlock.join(params, ", ")));
				}

				if("equals".equals(methodName) && parameters.length == 1 && parameters[0].getType().equalsToText(CommonClassNames.JAVA_LANG_OBJECT))
				{
					methodBuilder.addCode(CodeBlock.of("if(other == this) return true;\n"));

					methodBuilder.addCode(CodeBlock.of("if(other == null || other.getClass() != this.getClass()) return false;\n"));

					for(KtParameter param : primaryConstructorParameters)
					{
						methodBuilder.addCode("if(!$T.equals($L, (($T) other).$L)) return false;\n", Objects.class, param.getName(), thisTypeRef, param.getName());
					}

					methodBuilder.addCode("return true;");
				}

				if("toString".equals(methodName) && parameters.length == 0)
				{
					methodBuilder.addCode("$T __builder = new $T();\n", StringBuilder.class, StringBuilder.class);
					methodBuilder.addCode("__builder.append(\"$T(\");\n", thisTypeRef);

					int i = 0;
					for(KtParameter primaryConstructorParameter : primaryConstructorParameters)
					{
						StringBuilder param = new StringBuilder("__builder.append(\"");
						param.append(primaryConstructorParameter.getName());
						param.append("=\").append(");
						param.append(primaryConstructorParameter.getName());
						param.append(")");

						i++;

						if(i != primaryConstructorParameters.size())
						{
							param.append(".append(\",\")");
						}

						param.append(";\n");

						methodBuilder.addCode(param.toString());
					}
					methodBuilder.addCode("__builder.append(\")\");\n", thisTypeRef);
					methodBuilder.addCode(CodeBlock.of("return __builder.toString();"));
				}

				if(isConstructor)
				{
					for(PsiParameter parameter : parameters)
					{
						methodBuilder.addCode(CodeBlock.of("this.$L = $L;\n", parameter.getName(), parameter.getName()));
					}
				}

				if(methodName.equals("copy"))
				{
					List<CodeBlock> params = primaryConstructorParameters.stream().map(it -> CodeBlock.of("$L", safeName(it.getName()))).collect(Collectors.toList());

					methodBuilder.addCode(CodeBlock.of("return new $T($L);", thisTypeRef, CodeBlock.join(params, ", ")));
				}

				String component = "component";
				if(methodName.startsWith(component))
				{
					int index = Integer.parseInt(methodName.substring(component.length(), methodName.length()));
					KtParameter parameter = primaryConstructorParameters.get(index - 1);
					methodBuilder.addCode(CodeBlock.of("return $L;", parameter.getName()));
				}
				else
				{
					String propertyName = StringUtil.getPropertyName(methodName);
					if(propertyName != null)
					{
						Optional<KtParameter> optional = primaryConstructorParameters.stream().filter(it -> propertyName.equals(it.getName())).findFirst();

						if(methodName.startsWith("get") || methodName.startsWith("is"))
						{
							if(optional.isPresent())
							{
								methodBuilder.addCode(CodeBlock.of("return $L;", propertyName));
							}
						}
						else
						{
							methodBuilder.addCode(CodeBlock.of("this.$L = $L;", propertyName, propertyName));
						}
					}
				}
			}

			methodBuilders.accept(methodOrConstructor, methodBuilder);
		}

		return hasAnyChild;
	}

	public static String safeName(String name)
	{
		if(JavaLexer.isKeyword(name, LanguageLevel.JDK_1_8))
		{
			return "_" + name;
		}
		return name;
	}

	private static Modifier[] convertModifiers(PsiModifierListOwner owner)
	{
		return convertModifiers(owner, false);
	}

	private static Modifier[] convertModifiers(PsiModifierListOwner owner, boolean isInterface)
	{
		List<Modifier> modifiers = new ArrayList<>();
		if(isInterface)
		{
			if(owner.hasModifier(JvmModifier.STATIC))
			{
				return new Modifier[]{
						Modifier.PUBLIC,
						Modifier.STATIC
				};
			}

			return new Modifier[]{
					Modifier.PUBLIC,
					Modifier.ABSTRACT
			};
		}

		if(owner.hasModifier(JvmModifier.PUBLIC))
		{
			modifiers.add(Modifier.PUBLIC);
		}

		if(owner.hasModifier(JvmModifier.PRIVATE))
		{
			modifiers.add(Modifier.PRIVATE);
		}

		if(owner.hasModifier(JvmModifier.PROTECTED))
		{
			modifiers.add(Modifier.PROTECTED);
		}

		if(owner.hasModifier(JvmModifier.FINAL))
		{
			modifiers.add(Modifier.FINAL);
		}

		if(owner.hasModifier(JvmModifier.ABSTRACT))
		{
			modifiers.add(Modifier.ABSTRACT);
		}

		if(owner.hasModifier(JvmModifier.STATIC))
		{
			modifiers.add(Modifier.STATIC);
		}

		return modifiers.toArray(new Modifier[0]);
	}
}

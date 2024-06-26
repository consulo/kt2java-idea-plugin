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
import consulo.internal.mjga.idea.convert.expression.MethodCallExpression;
import consulo.internal.mjga.idea.convert.expression.StaticTypeQualifiedExpression;
import consulo.internal.mjga.idea.convert.expression.SuperExpression;
import consulo.internal.mjga.idea.convert.generate.KtToJavaClassBinder;
import consulo.internal.mjga.idea.convert.statement.ExpressionStatement;
import consulo.internal.mjga.idea.convert.statement.ReturnStatement;
import consulo.internal.mjga.idea.convert.statement.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport;
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade;
import org.jetbrains.kotlin.asJava.classes.KtUltraLightMethodForSourceDeclaration;
import org.jetbrains.kotlin.asJava.classes.KtUltraLightParameterForSource;
import org.jetbrains.kotlin.asJava.elements.KtLightElement;
import org.jetbrains.kotlin.asJava.elements.KtLightFieldForSourceDeclarationSupport;
import org.jetbrains.kotlin.psi.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2018-10-15
 */
public class MemberConverter
{
	public static final TypeName KOTLIN_UNIT = ClassName.get("kotlin", "Unit");

	private static final Class[] ourNullableAnnotations = {
			NotNull.class,
			Nullable.class
	};

	public static void run(ConvertContext context)
	{
		Collection<KtFile> files = context.getFiles();

		for (KtFile file : files)
		{
			convert(context, file);
		}
	}

	private static void convert(ConvertContext context, KtFile file)
	{
		KotlinAsJavaSupport kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(file.getProject());

		Collection<KtLightClassForFacade> facadeClassesInPackage = kotlinAsJavaSupport.getFacadeClassesInPackage(file.getPackageFqName(), file.getResolveScope());

		PsiClass fileClass = null;

		for (KtLightClassForFacade psiClass : facadeClassesInPackage)
		{
			Collection<KtFile> files = psiClass.getFiles();
			if (files.contains(file))
			{
				fileClass = psiClass;
				break;
			}
		}

		// just bind file to java class, may skipped if empty
		KtToJavaClassBinder fileClassBinder = context.bind(file);
		fileClassBinder.setJavaWrapper(fileClass);

		// bind classes
		for (KtDeclaration declaration : file.getDeclarations())
		{
			if (declaration instanceof KtClassOrObject)
			{
				PsiClass[] classes = JavaPsiFacade.getInstance(file.getProject()).findClasses(((KtClassOrObject) declaration).getFqName().toString(), file.getResolveScope());

				for (PsiClass clazz : classes)
				{
					if (clazz instanceof KtLightElement)
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
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		});
	}

	private static void writeFile(ConvertContext context, KtToJavaClassBinder binder) throws IOException
	{
		KtElement sourceElement = binder.getSourceElement();
		Module module = ReadAction.compute(() -> ModuleUtil.findModuleForPsiElement(sourceElement));
		if (module == null)
		{
			return;
		}

		ContentEntry entry = ReadAction.compute(() -> ModuleRootManager.getInstance(module).getContentEntries()[0]);
		SourceFolder sourceFolder = entry.getSourceFolders(JavaSourceRootType.SOURCE).get(0);

		VirtualFile scopeDir = sourceFolder.getFile();

		TypeSpec.Builder builder;

		boolean isInterface = false;
		boolean isEnum = false;
		switch (binder.getSourceClassType())
		{
			default:
				builder = TypeSpec.classBuilder(binder.getClassName());
				break;
			case INTERFACE:
				isInterface = true;
				builder = TypeSpec.interfaceBuilder(binder.getClassName());
				break;
			case ENUM:
				isEnum = true;
				builder = TypeSpec.enumBuilder(binder.getClassName());
				break;
		}

		boolean forcePublic = sourceElement instanceof KtFile;
		final boolean finalIsInterface = isInterface;
		final boolean finalIsEnum = isEnum;
		boolean containsAnyChild = ReadAction.compute(() -> convertClass(context, builder, binder.getJavaWrapper(), finalIsInterface, finalIsEnum, forcePublic));

		if (!containsAnyChild)
		{
			return;
		}

		JavaFile.Builder fileBuilder = JavaFile.builder(binder.getPackageName(), builder.build());

		JavaFile javaFile = fileBuilder.build();

		String text = javaFile.toString();

		WriteAction.runAndWait(() ->
		{
			VirtualFile targetDirectory = scopeDir;

			if (!StringUtil.isEmpty(binder.getPackageName()))
			{
				String directories = binder.getPackageName().replace(".", "/");

				targetDirectory = VfsUtil.createDirectoryIfMissing(targetDirectory, directories);
			}

			String file = binder.getClassName() + ".java";
			VirtualFile child = targetDirectory.findChild(file);
			if (child == null)
			{
				child = targetDirectory.createChildData(null, file);
			}
			child.setBinaryContent(text.getBytes(StandardCharsets.UTF_8));
		});
	}

	public static boolean convertClass(ConvertContext context, TypeSpec.Builder builder, PsiClass javaWrapper, boolean isInterface, boolean isEnum, boolean forcePublic)
	{
		if (!(javaWrapper instanceof PsiExtensibleClass))
		{
			return false;
		}

		boolean hasAnyChild = false;

		boolean isAnonym = javaWrapper instanceof PsiAnonymousClass;
		if (!isAnonym)
		{
			builder.addModifiers(convertModifiers(javaWrapper));
		}

		KtClassOrObject ktClassOrObject = null;
		if (javaWrapper instanceof KtLightElement)
		{
			KtElement kotlinOrigin = ((KtLightElement) javaWrapper).getKotlinOrigin();

			if (kotlinOrigin instanceof KtClassOrObject)
			{
				ktClassOrObject = (KtClassOrObject) kotlinOrigin;
			}
		}

		if (!isEnum)
		{
			JvmReferenceType superClass = javaWrapper.getSuperClassType();
			if (superClass != null)
			{
				builder.superclass(TypeConverter.convertJavaPsiType((PsiType) superClass));
			}
		}

		JvmReferenceType[] interfaceTypes = javaWrapper.getInterfaceTypes();
		for (JvmReferenceType interfaceType : interfaceTypes)
		{
			builder.addSuperinterface(TypeConverter.convertJavaPsiType((PsiType) interfaceType));
		}

		ClassName thisTypeRef = isAnonym ? null : ClassName.bestGuess(javaWrapper.getQualifiedName());

		for (PsiClass innerClass : javaWrapper.getInnerClasses())
		{
			if (!(innerClass instanceof PsiExtensibleClass))
			{
				continue;
			}

			if ("Companion".equals(innerClass.getName()))
			{
				hasAnyChild |= mapMembers(context,
						thisTypeRef,
						(PsiExtensibleClass) innerClass,
						ktClassOrObject,
						false,
						false,
						false,
						(p, f) ->
						{
							f.addModifiers(Modifier.STATIC);
							builder.addField(f.build());
						},
						(e, n) ->
						{
							throw new UnsupportedOperationException();
						},
						(p, m) ->
						{
							if (p.isConstructor())
							{
								// not constructor from companion object
								return;
							}
							m.addModifiers(Modifier.STATIC);
							builder.addMethod(m.build());
						},
						codeBlock -> {});
			}
			else
			{
				TypeSpec.Builder innerBuilder;

				boolean isInterface2 = false;
				boolean isEnum2 = false;

				if (innerClass.isInterface())
				{
					isInterface2 = true;
					innerBuilder = TypeSpec.interfaceBuilder(innerClass.getName());

				}
				else if (innerClass.isEnum())
				{
					isEnum2 = true;
					innerBuilder = TypeSpec.enumBuilder(innerClass.getName());
				}
				else
				{
					innerBuilder = TypeSpec.classBuilder(innerClass.getName());
				}

				final boolean finalIsInterface = isInterface2;
				final boolean finalIsEnum = isEnum2;

				convertClass(context, innerBuilder, innerClass, finalIsInterface, finalIsEnum, false);

				builder.addType(innerBuilder.build());
			}
		}

		hasAnyChild |= mapMembers(context,
				thisTypeRef,
				(PsiExtensibleClass) javaWrapper,
				ktClassOrObject,
				isInterface,
				forcePublic,
				isAnonym,
				(p, f) -> builder.addField(f.build()),
				(e, n) -> builder.addEnumConstant(n),
				(p, m) -> builder.addMethod(m.build()),
				codeBlock -> builder.addInitializerBlock(codeBlock));
		return hasAnyChild;
	}

	@SuppressWarnings("KotlinInternalInJava")
	private static boolean mapMembers(@NotNull ConvertContext context,
									  @Nullable TypeName thisTypeRef,
									  @NotNull PsiExtensibleClass javaWrapper,
									  @Nullable KtClassOrObject ktClassOrObject,
									  boolean isInterface,
									  boolean forcePublic,
									  boolean isAnonymClass,
									  @NotNull BiConsumer<PsiField, FieldSpec.Builder> fieldBuilders,
									  @NotNull BiConsumer<PsiEnumConstant, String> enumConsumer,
									  @NotNull BiConsumer<PsiMethod, MethodSpec.Builder> methodBuilders,
									  @NotNull Consumer<CodeBlock> initializerConsumer)
	{
		boolean hasAnyChild = false;

		List<PsiField> fields = javaWrapper.getOwnFields();
		for (PsiField field : fields)
		{
			// companion field
			if (field.hasModifier(JvmModifier.STATIC) && "Companion".equals(field.getName()))
			{
				continue;
			}

			hasAnyChild = true;

			if (field instanceof PsiEnumConstant)
			{
				enumConsumer.accept((PsiEnumConstant) field, field.getName());
			}
			else
			{
				FieldSpec.Builder fieldBuilder = FieldSpec.builder(TypeConverter.convertJavaPsiType(field.getType()), safeName(field.getName()), convertModifiers(field, isInterface, forcePublic));

				if (field instanceof KtLightFieldForSourceDeclarationSupport)
				{
					KtDeclaration kotlinOrigin = ((KtLightFieldForSourceDeclarationSupport) field).getKotlinOrigin();

					if (kotlinOrigin instanceof KtProperty)
					{
						KtExpression initializer = ((KtProperty) kotlinOrigin).getInitializer();

						if (initializer != null)
						{
							GeneratedElement codeBlock = ExpressionConverter.convertNonnull(initializer, context);
							fieldBuilder.initializer(codeBlock.generate());
						}
					}
				}

				if (ktClassOrObject instanceof KtObjectDeclaration && "INSTANCE".equals(field.getName()))
				{
					fieldBuilder.initializer(CodeBlock.of("new $T()", thisTypeRef));
				}

				fieldBuilders.accept(field, fieldBuilder);
			}
		}

		List<GeneratedElement> constructorInit = new ArrayList<>();
		if (ktClassOrObject != null)
		{
			List<KtAnonymousInitializer> anonymousInitializers = ktClassOrObject.getAnonymousInitializers();

			for (KtAnonymousInitializer anonymousInitializer : anonymousInitializers)
			{
				constructorInit.add(ExpressionConverter.convertNonnull(anonymousInitializer.getBody(), context));
			}
		}

		List<PsiMethod> ownMethods = javaWrapper.getOwnMethods();
		for (PsiMethod methodOrConstructor : ownMethods)
		{
			hasAnyChild = true;

			boolean isVoid = PsiTypes.voidType().equals(methodOrConstructor.getReturnType());

			KtValVarKeywordOwner ktPropertyOrParameter = null;
			KtDeclarationWithBody ktDeclarationWithBody = null;
			if (methodOrConstructor instanceof KtUltraLightMethodForSourceDeclaration)
			{
				LightMemberOrigin lightMemberOrigin = ((KtUltraLightMethodForSourceDeclaration) methodOrConstructor).getLightMemberOrigin();

				KtDeclaration originalElement = lightMemberOrigin == null ? null : lightMemberOrigin.getOriginalElement();

				if (originalElement instanceof KtFunction)
				{
					KtExpression bodyExpression = ((KtFunction) originalElement).getBodyExpression();

					if (bodyExpression != null)
					{
						ktDeclarationWithBody = (KtDeclarationWithBody) originalElement;
					}
					else
					{
						KtBlockExpression bodyBlockExpression = ((KtFunction) originalElement).getBodyBlockExpression();
						ktDeclarationWithBody = bodyBlockExpression == null ? null : (KtDeclarationWithBody) originalElement;
					}
				}
				else if (originalElement instanceof KtValVarKeywordOwner)
				{
					ktPropertyOrParameter = (KtValVarKeywordOwner) originalElement;
				}
			}

			boolean isConstructor = methodOrConstructor.isConstructor();

			if (isConstructor && isAnonymClass)
			{
				for (GeneratedElement element : constructorInit)
				{
					initializerConsumer.accept(element.generate(true));
				}
			}
			else
			{
				String methodName = safeName(methodOrConstructor.getName());
				MethodSpec.Builder methodBuilder = isConstructor ? MethodSpec.constructorBuilder() : MethodSpec.methodBuilder(methodName);
				methodBuilder.addModifiers(convertModifiers(methodOrConstructor, isInterface, forcePublic));

				if (!isConstructor)
				{
					methodBuilder.returns(TypeConverter.convertJavaPsiType(methodOrConstructor.getReturnType()));

					for (Class annotation : ourNullableAnnotations)
					{
						if (methodOrConstructor.hasAnnotation(annotation.getName()))
						{
							methodBuilder.addAnnotation(annotation);
						}
					}
				}

				PsiParameter[] parameters = methodOrConstructor.getParameterList().getParameters();
				for (PsiParameter parameter : parameters)
				{
					ParameterSpec.Builder paramSpec = ParameterSpec.builder(TypeConverter.convertJavaPsiType(parameter.getType()), safeName(parameter.getName()));
					for (Class annotation : ourNullableAnnotations)
					{
						if (parameter.hasAnnotation(annotation.getName()))
						{
							paramSpec.addAnnotation(annotation);
						}
					}
					methodBuilder.addParameter(paramSpec.build());
				}

				JvmReferenceType[] throwsTypes = methodOrConstructor.getThrowsTypes();
				for (JvmReferenceType throwsType : throwsTypes)
				{
					methodBuilder.addException(TypeConverter.convertJavaPsiType((PsiType) throwsType));
				}

				if (ktDeclarationWithBody != null)
				{
					setBody(methodBuilder, ktDeclarationWithBody, context, isVoid);
				}
				else if (ktPropertyOrParameter != null && !methodOrConstructor.hasModifier(JvmModifier.ABSTRACT))
				{
					String name = safeName(((PsiNamedElement) ktPropertyOrParameter).getName());

					if (methodName.startsWith("get") || methodName.startsWith("is"))
					{
						boolean wantDefault = true;
						if (ktPropertyOrParameter instanceof KtProperty)
						{
							KtPropertyAccessor getter = ((KtProperty) ktPropertyOrParameter).getGetter();
							if (getter != null)
							{
								wantDefault = false;

								setBody(methodBuilder, getter, context, false);
							}
						}

						if (wantDefault)
						{
							methodBuilder.addCode(CodeBlock.of("return $L;", name));
						}
					}
					else
					{
						boolean wantDefault = true;
						if (ktPropertyOrParameter instanceof KtProperty)
						{
							KtPropertyAccessor setter = ((KtProperty) ktPropertyOrParameter).getSetter();
							if (setter != null)
							{
								wantDefault = false;

								setBody(methodBuilder, setter, context, false);
							}
						}

						if (wantDefault)
						{
							methodBuilder.addCode(CodeBlock.of("this.$L = $L;", name, name));
						}
					}
				}
				else if (isConstructor && ktClassOrObject instanceof KtClass && !ktClassOrObject.isData())
				{
					KtPrimaryConstructor primaryConstructor = ktClassOrObject.getPrimaryConstructor();

					List<KtSuperTypeListEntry> superTypeListEntries = ktClassOrObject.getSuperTypeListEntries();
					for (KtSuperTypeListEntry superTypeListEntry : superTypeListEntries)
					{
						if (superTypeListEntry instanceof KtSuperTypeCallEntry)
						{
							//KtConstructorCalleeExpression calleeExpression = ((KtSuperTypeCallEntry) superTypeListEntry).getCalleeExpression();

							List<? extends ValueArgument> valueArguments = ((KtSuperTypeCallEntry) superTypeListEntry).getValueArguments();

							List<GeneratedElement> args = new ArrayList<>();
							for (ValueArgument argument : valueArguments)
							{
								args.add(ExpressionConverter.convertNonnull(argument.getArgumentExpression(), context));
							}

							MethodCallExpression expression = new MethodCallExpression(new SuperExpression(), args);
							methodBuilder.addCode(new ExpressionStatement(expression).wantSemicolon(true).generate(true));
						}
					}

					for (PsiParameter parameter : parameters)
					{
						if (parameter instanceof KtUltraLightParameterForSource)
						{
							KtParameter sourceElement = ((KtUltraLightParameterForSource) parameter).getKotlinOrigin();

							// if var or val not set - it just reference parameter, without backend field
							if (sourceElement.getValOrVarKeyword() == null)
							{
								continue;
							}
						}
						methodBuilder.addCode(CodeBlock.of("this.$L = $L;\n", parameter.getName(), parameter.getName()));
					}

					for (GeneratedElement element : constructorInit)
					{
						methodBuilder.addCode(element.generate(true));
					}
				}
				else if (ktClassOrObject instanceof KtClass && ktClassOrObject.isData())
				{
					List<KtParameter> primaryConstructorParameters = ktClassOrObject.getPrimaryConstructorParameters();

					if ("hashCode".equals(methodName) && parameters.length == 0)
					{
						List<CodeBlock> params = primaryConstructorParameters.stream().map(it -> CodeBlock.of("$L", safeName(it.getName()))).collect(Collectors.toList());

						methodBuilder.addCode(CodeBlock.of("return $T.hash($L);", Objects.class, CodeBlock.join(params, ", ")));
					}

					if ("equals".equals(methodName) && parameters.length == 1 && parameters[0].getType().equalsToText(CommonClassNames.JAVA_LANG_OBJECT))
					{
						methodBuilder.addCode(CodeBlock.of("if(other == this) return true;\n"));

						methodBuilder.addCode(CodeBlock.of("if(other == null || other.getClass() != this.getClass()) return false;\n"));

						for (KtParameter param : primaryConstructorParameters)
						{
							methodBuilder.addCode("if(!$T.equals($L, (($T) other).$L)) return false;\n", Objects.class, param.getName(), thisTypeRef, param.getName());
						}

						methodBuilder.addCode("return true;");
					}

					if ("toString".equals(methodName) && parameters.length == 0)
					{
						methodBuilder.addCode("$T __builder = new $T();\n", StringBuilder.class, StringBuilder.class);
						methodBuilder.addCode("__builder.append(\"$T(\");\n", thisTypeRef);

						int i = 0;
						for (KtParameter primaryConstructorParameter : primaryConstructorParameters)
						{
							StringBuilder param = new StringBuilder("__builder.append(\"");
							param.append(primaryConstructorParameter.getName());
							param.append("=\").append(");
							param.append(primaryConstructorParameter.getName());
							param.append(")");

							i++;

							if (i != primaryConstructorParameters.size())
							{
								param.append(".append(\",\")");
							}

							param.append(";\n");

							methodBuilder.addCode(param.toString());
						}
						methodBuilder.addCode("__builder.append(\")\");\n", thisTypeRef);
						methodBuilder.addCode(CodeBlock.of("return __builder.toString();"));
					}

					if (isConstructor)
					{
						for (PsiParameter parameter : parameters)
						{
							methodBuilder.addCode(CodeBlock.of("this.$L = $L;\n", parameter.getName(), parameter.getName()));
						}
					}

					if (methodName.equals("copy"))
					{
						List<CodeBlock> params = primaryConstructorParameters.stream().map(it -> CodeBlock.of("$L", safeName(it.getName()))).collect(Collectors.toList());

						methodBuilder.addCode(CodeBlock.of("return new $T($L);", thisTypeRef, CodeBlock.join(params, ", ")));
					}

					String component = "component";
					if (methodName.startsWith(component))
					{
						int index = Integer.parseInt(methodName.substring(component.length()));
						KtParameter parameter = primaryConstructorParameters.get(index - 1);
						methodBuilder.addCode(CodeBlock.of("return $L;", parameter.getName()));
					}
					else
					{
						String propertyName = StringUtil.getPropertyName(methodName);
						if (propertyName != null)
						{
							Optional<KtParameter> optional = primaryConstructorParameters.stream().filter(it -> propertyName.equals(it.getName())).findFirst();

							if (methodName.startsWith("get") || methodName.startsWith("is"))
							{
								if (optional.isPresent())
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
		}

		return hasAnyChild;
	}

	private static void setBody(MethodSpec.Builder methodBuilder, KtDeclarationWithBody declarationWithBody, ConvertContext context, boolean isVoid)
	{
		KtElement body = declarationWithBody.getBodyBlockExpression();
		if (body == null)
		{
			body = declarationWithBody.getBodyExpression();
		}

		GeneratedElement inner;
		GeneratedElement generatedElement = ExpressionConverter.convertNonnull(body, context);
		if (generatedElement instanceof Statement)
		{
			inner = generatedElement;
		}
		else
		{
			if(isVoid && isUnitReturn(generatedElement))
			{
				return;
			}

			inner = new ReturnStatement(generatedElement).wantSemicolon(true);
		}

//		List<TryCatchStatement.Catch> list = new ArrayList<>();
//		ThrowStatement rethrow = new ThrowStatement(new NewExpression(TypeName.get(RuntimeException.class), Collections.singletonList(new ReferenceExpression("__e"))));
//		list.add(new TryCatchStatement.Catch("__e", TypeName.get(Exception.class), rethrow));
//
//		TryCatchStatement statement = new TryCatchStatement(inner, list);

		methodBuilder.addCode(inner.generate());
	}

	private static boolean isUnitReturn(GeneratedElement element)
	{
		if(element instanceof StaticTypeQualifiedExpression qualifiedExpression)
		{
			TypeName typeName = qualifiedExpression.getTypeName();
			if(KOTLIN_UNIT.equals(typeName))
			{
				return true;
			}
		}

		return false;
	}

	public static String safeName(String name)
	{
		if (JavaLexer.isKeyword(name, LanguageLevel.JDK_1_8))
		{
			return "_" + name;
		}
		return name;
	}

	private static Modifier[] convertModifiers(PsiModifierListOwner owner)
	{
		return convertModifiers(owner, false, false);
	}

	private static Modifier[] convertModifiers(PsiModifierListOwner owner, boolean isInterface, boolean forcePublic)
	{
		List<Modifier> modifiers = new ArrayList<>();
		if (isInterface)
		{
			if (owner.hasModifier(JvmModifier.STATIC))
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

		if (!forcePublic)
		{
			if (owner.hasModifier(JvmModifier.PUBLIC))
			{
				modifiers.add(Modifier.PUBLIC);
			}

			if (owner.hasModifier(JvmModifier.PRIVATE))
			{
				modifiers.add(Modifier.PRIVATE);
			}

			if (owner.hasModifier(JvmModifier.PROTECTED))
			{
				modifiers.add(Modifier.PROTECTED);
			}
		}
		else
		{
			modifiers.add(Modifier.PUBLIC);
		}

		if (owner.hasModifier(JvmModifier.FINAL) && !(owner.hasModifier(JvmModifier.STATIC) || owner.hasModifier(JvmModifier.PRIVATE)))
		{
			modifiers.add(Modifier.FINAL);
		}

		if (owner.hasModifier(JvmModifier.ABSTRACT))
		{
			modifiers.add(Modifier.ABSTRACT);
		}

		if (owner.hasModifier(JvmModifier.STATIC))
		{
			modifiers.add(Modifier.STATIC);
		}

		if (owner.hasModifier(JvmModifier.SYNCHRONIZED))
		{
			modifiers.add(Modifier.SYNCHRONIZED);
		}

		if (owner.hasModifier(JvmModifier.VOLATILE))
		{
			modifiers.add(Modifier.VOLATILE);
		}

		return modifiers.toArray(new Modifier[0]);
	}
}

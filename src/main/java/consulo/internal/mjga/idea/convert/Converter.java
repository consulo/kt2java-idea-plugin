package consulo.internal.mjga.idea.convert;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import consulo.internal.mjga.idea.convert.generate.KtToJavaClassBinder;
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 2018-10-15
 */
public class Converter
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

		// bind classes
		for(KtDeclaration declaration : file.getDeclarations())
		{
			if(declaration instanceof KtClassOrObject)
			{
				PsiClass aClass = JavaPsiFacade.getInstance(file.getProject()).findClass(((KtClassOrObject) declaration).getFqName().toString(), file.getResolveScope());

				if(aClass != null)
				{
					context.bind((KtClassOrObject) declaration);
				}
			}
		}


		System.out.println("test");
	}
}

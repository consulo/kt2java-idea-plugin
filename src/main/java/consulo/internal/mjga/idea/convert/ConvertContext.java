package consulo.internal.mjga.idea.convert;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.internal.mjga.idea.convert.generate.JavaSourceClassType;
import consulo.internal.mjga.idea.convert.generate.KtToJavaClassBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService;
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2018-10-15
 */
public class ConvertContext
{
	private final Project myProject;
	private final Map<VirtualFile, KtFile> myVfToPsiFile;

	private Map<KtFile, KtToJavaClassBinder> myFileBinder = new HashMap<>();
	private Map<KtClassOrObject, KtToJavaClassBinder> myClassBinder = new HashMap<>();

	public ConvertContext(Project project, Map<VirtualFile, KtFile> vfToPsiFile)
	{
		myProject = project;
		myVfToPsiFile = vfToPsiFile;
	}

	@NotNull
	public KtToJavaClassBinder bind(KtFile file)
	{
		return myFileBinder.computeIfAbsent(file, f -> {
			FqName packageFqName = file.getPackageFqName();
			String className = StringUtil.capitalize(f.getVirtualFile().getNameWithoutExtension()) + "Kt";
			return new KtToJavaClassBinder(packageFqName.toString(), JavaSourceClassType.CLASS, className, file);
		});
	}

	@NotNull
	public KtToJavaClassBinder bind(KtClassOrObject ktClass)
	{
		return myClassBinder.computeIfAbsent(ktClass, f -> {
			KtFile containingKtFile = f.getContainingKtFile();
			FqName packageFqName = containingKtFile.getPackageFqName();
			String className = f.getName();
			JavaSourceClassType classType = JavaSourceClassType.CLASS;
			if(ktClass instanceof KtClass)
			{
				if(((KtClass)f).isInterface())
				{
					classType = JavaSourceClassType.INTERFACE;
				}
				else if(((KtClass) f).isEnum())
				{
					classType = JavaSourceClassType.ENUM;
				}
			}
			return new KtToJavaClassBinder(packageFqName.toString(), classType, className, f);
		});
	}

	public Project getProject()
	{
		return myProject;
	}

	public Collection<KtFile> getFiles()
	{
		return myVfToPsiFile.values();
	}

	public void forEach(Consumer<KtToJavaClassBinder> binder)
	{
		for(KtToJavaClassBinder classBinder : myFileBinder.values())
		{
			binder.accept(classBinder);
		}

		for(KtToJavaClassBinder classBinder : myClassBinder.values())
		{
			binder.accept(classBinder);
		}
	}
}

package consulo.internal.mjga.idea.convert.generate;

import com.intellij.psi.PsiClass;
import org.jetbrains.kotlin.psi.KtElement;

/**
 * @author VISTALL
 * @since 2018-10-15
 */
public class KtToJavaClassBinder
{
	private final String myPackageName;
	private final JavaSourceClassType mySourceClassType;
	private final String myClassName;

	private KtElement mySourceElement;

	private PsiClass myJavaWrapper;

	public KtToJavaClassBinder(String packageName, JavaSourceClassType sourceClassType, String className, KtElement sourceElement)
	{
		myPackageName = packageName;
		mySourceClassType = sourceClassType;
		myClassName = className;
		mySourceElement = sourceElement;
	}

	public void setJavaWrapper(PsiClass javaWrapper)
	{
		myJavaWrapper = javaWrapper;
	}

	public PsiClass getJavaWrapper()
	{
		return myJavaWrapper;
	}

	public KtElement getSourceElement()
	{
		return mySourceElement;
	}

	public String getPackageName()
	{
		return myPackageName;
	}

	public String getClassName()
	{
		return myClassName;
	}

	public JavaSourceClassType getSourceClassType()
	{
		return mySourceClassType;
	}
}

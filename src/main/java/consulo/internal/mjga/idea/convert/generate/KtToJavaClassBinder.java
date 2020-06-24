package consulo.internal.mjga.idea.convert.generate;

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

	public KtToJavaClassBinder(String packageName, JavaSourceClassType sourceClassType, String className, KtElement sourceElement)
	{
		myPackageName = packageName;
		mySourceClassType = sourceClassType;
		myClassName = className;
		mySourceElement = sourceElement;
	}
}

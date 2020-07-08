package consulo.internal.mjga.idea.convert;

import com.squareup.javapoet.CodeBlock;

/**
 * @author VISTALL
 * @since 2020-07-07
 */
public abstract class GeneratedElement
{
	public CodeBlock generate()
	{
		return generate(false);
	}

	public abstract CodeBlock generate(boolean needNewLine);

	public static String wrap(String text, boolean needNewLine)
	{
		return needNewLine ? text + "\n" : text;
	}
}

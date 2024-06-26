package consulo.internal.mjga.idea.convert;

import com.intellij.openapi.util.UserDataHolderBase;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 2020-07-07
 */
public abstract class GeneratedElement extends UserDataHolderBase
{
	private boolean myWantSemicolon;

	public CodeBlock generate()
	{
		return generate(false);
	}

	public abstract CodeBlock generate(boolean needNewLine);

	@NotNull
	public String wrap(String text, boolean needNewLine)
	{
		if(text.endsWith(";") || text.endsWith("\n"))
		{
			throw new UnsupportedOperationException();
		}

		StringBuilder builder = new StringBuilder(text);
		if(myWantSemicolon && isAllowSemicolon())
		{
			builder.append(";");
		}

		if(needNewLine)
		{
			builder.append("\n");
		}
		return builder.toString();
	}

	protected boolean isAllowSemicolon()
	{
		return false;
	}

	public GeneratedElement wantSemicolon(boolean value)
	{
		myWantSemicolon = value;
		return this;
	}

	public GeneratedElement modifyToByExtensionCall(GeneratedElement receiverGenerate, TypeName qualifiedType)
	{
		return this;
	}
}

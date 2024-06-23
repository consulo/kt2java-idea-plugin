package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 23-Jun-24
 */
public class SynchronizedStatement extends Statement
{
	private final GeneratedElement myTarget;
	private final GeneratedElement myBody;

	public SynchronizedStatement(GeneratedElement target, GeneratedElement body)
	{
		myTarget = target;
		myBody = body;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.beginControlFlow("synchronized($L)", myTarget.generate());
		myBody.wantSemicolon(true);
		builder.add(myBody.generate(needNewLine));
		builder.endControlFlow();
		return builder.build();
	}
}

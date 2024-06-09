package consulo.internal.mjga.idea.convert.kotlinExp;

import consulo.internal.mjga.idea.convert.ConvertContext;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import org.jetbrains.kotlin.psi.KtElement;

public abstract class ExpressionAnalyzer<E extends KtElement>
{
	public abstract GeneratedElement analyze(E expression, ConvertContext context);
}

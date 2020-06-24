package consulo.internal.mjga.idea.actions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.psi.KtFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import consulo.internal.mjga.idea.convert.ConvertContext;
import consulo.internal.mjga.idea.convert.Converter;

/**
 * @author VISTALL
 * @since 2018-10-13
 */
public class ConvertToJavaAction extends AnAction
{
	public ConvertToJavaAction()
	{
		super("Convert Kotlin File to Java File");
	}

	@Override
	public void update(AnActionEvent e)
	{
		VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
		Project project = e.getProject();
		e.getPresentation().setEnabled(project != null && !collectKotlinFiles(files).isEmpty());
	}

	@Override
	public void actionPerformed(AnActionEvent e)
	{
		Project project = e.getProject();

		VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
		if(files == null || files.length == 0 || project == null)
		{
			return;
		}

		List<VirtualFile> kotlinFiles = collectKotlinFiles(files);
		if(kotlinFiles.isEmpty())
		{
			return;
		}

		new Task.Backgroundable(project, "Converting kotlin files...", true)
		{
			@Override
			public void run(@NotNull ProgressIndicator indicator)
			{
				ReadAction.run(() ->
				{
					PsiManager psiManager = PsiManager.getInstance(project);

					Map<VirtualFile, KtFile> vfToPsiFile = new LinkedHashMap<>();
					for(VirtualFile kotlinFile : kotlinFiles)
					{
						PsiFile file = psiManager.findFile(kotlinFile);
						if(file instanceof KtFile)
						{
							vfToPsiFile.put(kotlinFile, (KtFile) file);
						}
					}

					ConvertContext convertContext = new ConvertContext(project, vfToPsiFile);
					Converter.run(convertContext);
				});
			}
		}.queue();
	}

	private static List<VirtualFile> collectKotlinFiles(VirtualFile[] virtualFiles)
	{
		ArrayList<VirtualFile> files = new ArrayList<>();
		collectKotlinFiles(virtualFiles, files);
		return files;
	}

	private static void collectKotlinFiles(VirtualFile[] virtualFiles, List<VirtualFile> files)
	{
		for(VirtualFile child : virtualFiles)
		{
			if(child.isDirectory())
			{
				collectKotlinFiles(child.getChildren(), files);
			}
			else if(child.getFileType() == KotlinFileType.INSTANCE)
			{
				files.add(child);
			}
		}
	}
}

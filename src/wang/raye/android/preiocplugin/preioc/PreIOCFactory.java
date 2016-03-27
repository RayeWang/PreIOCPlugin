package wang.raye.android.preiocplugin.preioc;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wang.raye.android.preiocplugin.common.Utils;

/**
 * Factory for obtaining proper ButterKnife version.
 *
 * @author Raye
 * @since 1.3
 */
public class PreIOCFactory {

    /**
     * List of supported ButterKnifes.
     * Note: The ordering corresponds to the preferred ButterKnife versions.
     */
    private static IPreIOC[] sSupportedButterKnives = new IPreIOC[]{
            new PreIOC106()
    };

    private PreIOCFactory() {
        // no construction
    }

    /**
     * Find ButterKnife that is available for given {@link com.intellij.psi.PsiElement} in the {@link com.intellij.openapi.project.Project}.
     * Note that it check if ButterKnife is available in the module.
     *
     * @param project    Project
     * @param psiElement Element for which we are searching for ButterKnife
     * @return ButterKnife
     */
    @Nullable
    public static IPreIOC findPreIOCForPsiElement(@NotNull Project project, @NotNull PsiElement psiElement) {
        for (IPreIOC preIOC : sSupportedButterKnives) {
            if (Utils.isClassAvailableForPsiFile(project, psiElement, preIOC.getDistinctClassName())) {
                return preIOC;
            }
        }
        // we haven't found any version of ButterKnife in the module, let's fallback to the whole project
        return findPreIOCForProject(project);
    }

    /**
     * Find ButterKnife that is available in the {@link com.intellij.openapi.project.Project}.
     *
     * @param project Project
     * @return ButterKnife
     * @since 1.3.1
     */
    @Nullable
    private static IPreIOC findPreIOCForProject(@NotNull Project project) {
        for (IPreIOC preIOC : sSupportedButterKnives) {
            if (Utils.isClassAvailableForProject(project, preIOC.getDistinctClassName())) {
                return preIOC;
            }
        }
        return null;
    }

    public static IPreIOC[] getSupportedButterKnives() {
        return sSupportedButterKnives;
    }
}

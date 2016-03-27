package wang.raye.android.preiocplugin.navgation;

import com.google.common.base.Predicate;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.AnnotatedMembersSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wang.raye.android.preiocplugin.preioc.IPreIOC;
import wang.raye.android.preiocplugin.preioc.PreIOCFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationMarkerProvider implements LineMarkerProvider {

    private static final Predicate<PsiElement> IS_FIELD_IDENTIFIER = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement element) {
            return element != null && element instanceof PsiIdentifier && element.getParent() instanceof PsiField;
        }
    };

    private static final Predicate<PsiElement> IS_METHOD_IDENTIFIER = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement element) {
            return element != null && element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod;
        }
    };

    /**
     * Check if element is a method annotated with <em>@OnClick</em> or a field annotated with
     * <em>@InjectView</em> and create corresponding navigation link.
     *
     * @return a {@link com.intellij.codeInsight.daemon.GutterIconNavigationHandler} for the
     * appropriate type, or null if we don't care about it.
     */
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull final PsiElement element) {
        final IPreIOC preIOC = PreIOCFactory.findPreIOCForPsiElement(element.getProject(), element);
        if (preIOC == null) {
            return null;
        }
        if (IS_FIELD_IDENTIFIER.apply(element)) {
            return getNavigationLineMarker((PsiIdentifier) element,
                    PreIOCLink.getButterKnifeLink(preIOC, IS_FIELD_IDENTIFIER));
        } else if (IS_METHOD_IDENTIFIER.apply(element)) {
            return getNavigationLineMarker((PsiIdentifier) element,
                    PreIOCLink.getButterKnifeLink(preIOC, IS_METHOD_IDENTIFIER));
        }

        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> lineMarkerInfos) {
        // empty
    }

    @Nullable
    private LineMarkerInfo getNavigationLineMarker(@NotNull final PsiIdentifier element, @Nullable PreIOCLink link) {
        if (link == null) {
            return null;
        }
        final PsiAnnotation srcAnnotation = PsiHelper.getAnnotation(element.getParent(), link.srcAnnotation);
        if (srcAnnotation != null) {
            final PsiAnnotationParameterList annotationParameters = srcAnnotation.getParameterList();
            if (annotationParameters.getAttributes().length > 0) {
                final PsiAnnotationMemberValue value = annotationParameters.getAttributes()[0].getValue();
                if (value == null) {
                    return null;
                }
                final String resourceId = value.getText();

                final PsiClass dstAnnotationClass = JavaPsiFacade.getInstance(element.getProject())
                        .findClass(link.dstAnnotation, ProjectScope.getLibrariesScope(element.getProject()));
                if (dstAnnotationClass == null) {
                    return null;
                }

                final ClassMemberProcessor processor = new ClassMemberProcessor(resourceId, link);

                AnnotatedMembersSearch.search(dstAnnotationClass,
                        GlobalSearchScope.fileScope(element.getContainingFile())).forEach(processor);
                final PsiMember dstMember = processor.getResultMember();
                if (dstMember != null) {
                    return new NavigationMarker.Builder().from(element).to(dstMember).build();
                }
            }
        }

        return null;
    }

    private static class PreIOCLink {
        private static final Map<IPreIOC, Map<Predicate<PsiElement>, PreIOCLink>> sMap =
                new HashMap<IPreIOC, Map<Predicate<PsiElement>, PreIOCLink>>(
                        PreIOCFactory.getSupportedButterKnives().length);

        static {
            for (IPreIOC preIOC : PreIOCFactory.getSupportedButterKnives()) {
                Map<Predicate<PsiElement>, PreIOCLink> sButterKnifeSubMap =
                        new HashMap<Predicate<PsiElement>, PreIOCLink>(2);
                sMap.put(preIOC, sButterKnifeSubMap);

                sButterKnifeSubMap.put(IS_FIELD_IDENTIFIER,
                        new PreIOCLink(preIOC.getFieldAnnotationCanonicalName(),
                                preIOC.getOnClickAnnotationCanonicalName()));
                sButterKnifeSubMap.put(IS_METHOD_IDENTIFIER,
                        new PreIOCLink(preIOC.getOnClickAnnotationCanonicalName(),
                                preIOC.getFieldAnnotationCanonicalName()));
            }
        }

        private final String srcAnnotation;
        private final String dstAnnotation;

        public PreIOCLink(String srcAnnotation, String dstAnnotation) {
            this.srcAnnotation = srcAnnotation;
            this.dstAnnotation = dstAnnotation;
        }

        @Nullable
        public static PreIOCLink getButterKnifeLink(@Nullable IPreIOC preIOC,
                                                         @NotNull Predicate<PsiElement> predicate) {
            Map<Predicate<PsiElement>, PreIOCLink> subMap = sMap.get(preIOC);
            if (subMap != null) {
                return subMap.get(predicate);
            }
            return null;
        }
    }

    private class ClassMemberProcessor implements Processor<PsiMember> {
        private final String resourceId;
        private final PreIOCLink link;
        private PsiMember resultMember;

        public ClassMemberProcessor(@NotNull final String resourceId, @NotNull final PreIOCLink link) {
            this.resourceId = resourceId;
            this.link = link;
        }

        @Override
        public boolean process(PsiMember psiMember) {
            if (PsiHelper.hasAnnotationWithValue(psiMember, link.dstAnnotation, resourceId)) {
                resultMember = psiMember;
                return false;
            }
            return true;
        }

        @Nullable
        public PsiMember getResultMember() {
            return resultMember;
        }
    }
}

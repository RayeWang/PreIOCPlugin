package wang.raye.android.preiocplugin;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.EverythingGlobalScope;
import org.jetbrains.annotations.NotNull;
import wang.raye.android.preiocplugin.common.Definitions;
import wang.raye.android.preiocplugin.common.Utils;
import wang.raye.android.preiocplugin.model.Element;
import wang.raye.android.preiocplugin.preioc.IPreIOC;
import wang.raye.android.preiocplugin.preioc.PreIOCFactory;

import java.util.ArrayList;

public class InjectWriter extends WriteCommandAction.Simple {

    protected PsiFile mFile;
    protected Project mProject;
    protected PsiClass mClass;
    protected ArrayList<Element> mElements;
    protected PsiElementFactory mFactory;
    protected String mLayoutFileName;
    protected String mFieldNamePrefix;
    protected boolean mCreateHolder;

    public InjectWriter(PsiFile file, PsiClass clazz, String command, ArrayList<Element> elements, String layoutFileName, String fieldNamePrefix, boolean createHolder) {
        super(clazz.getProject(), command);

        mFile = file;
        mProject = clazz.getProject();
        mClass = clazz;
        mElements = elements;
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        mLayoutFileName = layoutFileName;
        mFieldNamePrefix = fieldNamePrefix;
        mCreateHolder = createHolder;
    }

    @Override
    public void run() throws Throwable {
        final IPreIOC preIOC = PreIOCFactory.findPreIOCForPsiElement(mProject, mFile);
        if (preIOC == null) {
            return; // Butterknife library is not available for project
        }

        if (mCreateHolder) {
            generateAdapter(preIOC);
        } else {
            if (Utils.getInjectCount(mElements) > 0) {
                generateFields(preIOC);
            }
            generateInjects(preIOC);
            if (Utils.getClickCount(mElements) > 0) {
                generateClick();
            }
            Utils.showInfoNotification(mProject, String.valueOf(Utils.getInjectCount(mElements)) + " injections and " + String.valueOf(Utils.getClickCount(mElements)) + " onClick added to " + mFile.getName());
        }

        // reformat class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
    }

    protected void generateClick() {
        StringBuilder method = new StringBuilder();
        if (Utils.getClickCount(mElements) == 1) {
            method.append("@OnClick(");
            for (Element element : mElements) {
                if (element.isClick) {
                    method.append(element.getFullID() + ")");
                }
            }
            method.append("public void onClick() {}");
        } else {
            method.append("@OnClick({");
            int currentCount = 0;
            for (Element element : mElements) {
                if (element.isClick) {
                    currentCount++;
                    if (currentCount == Utils.getClickCount(mElements)) {
                        method.append(element.getFullID() + "})");
                    } else {
                        method.append(element.getFullID() + ",");
                    }
                }
            }
            method.append("public void onClick(android.view.View view) {switch (view.getId()){");
            for (Element element : mElements) {
                if (element.isClick) {
                    method.append("case " + element.getFullID() + ": break;");
                }
            }
            method.append("}}");
        }

        mClass.add(mFactory.createMethodFromText(method.toString(), mClass));

    }

    /**
     * Create ViewHolder for adapters with injections
     */
    protected void generateAdapter(@NotNull IPreIOC preIOC) {
        // view holder class
        StringBuilder holderBuilder = new StringBuilder();
        holderBuilder.append(Utils.getViewHolderClassName());
        holderBuilder.append("(android.view.View view) {");
        holderBuilder.append(preIOC.getCanonicalBindStatement());
        holderBuilder.append("(this, view);");
        holderBuilder.append("}");

        PsiClass viewHolder = mFactory.createClassFromText(holderBuilder.toString(), mClass);
        viewHolder.setName(Utils.getViewHolderClassName());

        // add injections into view holder
        for (Element element : mElements) {
            if (!element.used) {
                continue;
            }

            String rPrefix;
            if (element.isAndroidNS) {
                rPrefix = "android.R.id.";
            } else {
                rPrefix = "R.id.";
            }

            StringBuilder injection = new StringBuilder();
            injection.append('@');
            injection.append(preIOC.getFieldAnnotationCanonicalName());
            injection.append('(');
            injection.append(rPrefix);
            injection.append(element.id);
            injection.append(") ");
            if (element.nameFull != null && element.nameFull.length() > 0) { // custom package+class
                injection.append(element.nameFull);
            } else if (Definitions.paths.containsKey(element.name)) { // listed class
                injection.append(Definitions.paths.get(element.name));
            } else { // android.widget
                injection.append("android.widget.");
                injection.append(element.name);
            }
            injection.append(" ");
            injection.append(element.fieldName);
            injection.append(";");

            viewHolder.add(mFactory.createFieldFromText(injection.toString(), mClass));
        }

        mClass.add(viewHolder);

        // add view holder's comment
        StringBuilder comment = new StringBuilder();
        comment.append("/**\n");
        comment.append(" * This class contains all preIOC-injected Views & Layouts from layout file '");
        comment.append(mLayoutFileName);
        comment.append("'\n");
        comment.append("* for easy to all layout elements.\n");
        comment.append(" *\n");
        comment.append(" * @author\tPreIOC, plugin for Android Studio by Avast Developers (http://github.com/rayewang)\n");
        comment.append("*/");

//        mClass.addBefore(mFactory.createCommentFromText(comment.toString(), mClass), mClass.findInnerClassByName(Utils.getViewHolderClassName(), true));
        mClass.addBefore(mFactory.createKeyword("static", mClass), mClass.findInnerClassByName(Utils.getViewHolderClassName(), true));
    }

    /**
     * Create fields for injections inside main class
     */
    protected void generateFields(@NotNull IPreIOC preIOC) {
        // add injections into main class
        for (Element element : mElements) {
            if (!element.used) {
                continue;
            }

            StringBuilder injection = new StringBuilder();
            injection.append('@');
            injection.append(preIOC.getFieldAnnotationCanonicalName());
            injection.append('(');
            injection.append(element.getFullID());
            injection.append(") ");
            if (element.nameFull != null && element.nameFull.length() > 0) { // custom package+class
                injection.append(element.nameFull);
            } else if (Definitions.paths.containsKey(element.name)) { // listed class
                injection.append(Definitions.paths.get(element.name));
            } else { // android.widget
                injection.append("android.widget.");
                injection.append(element.name);
            }
            injection.append(" ");
            injection.append(element.fieldName);
            injection.append(";");

            mClass.add(mFactory.createFieldFromText(injection.toString(), mClass));
        }
    }

    private boolean containsButterKnifeInjectLine(PsiMethod method, String line) {
        final PsiCodeBlock body = method.getBody();
        if (body == null) {
            return false;
        }
        PsiStatement[] statements = body.getStatements();
        for (PsiStatement psiStatement : statements) {
            String statementAsString = psiStatement.getText();
            if (psiStatement instanceof PsiExpressionStatement && (statementAsString.contains(line))) {
                return true;
            }
        }
        return false;
    }

    protected void generateInjects(@NotNull IPreIOC preIOC) {
        PsiClass activityClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Activity", new EverythingGlobalScope(mProject));
        PsiClass fragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Fragment", new EverythingGlobalScope(mProject));
        PsiClass supportFragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.support.v4.app.Fragment", new EverythingGlobalScope(mProject));

        // Check for Activity class
        if (activityClass != null && mClass.isInheritor(activityClass, true)) {
            if (mClass.findMethodsByName("onCreate", false).length == 0) {
                // Add an empty stub of onCreate()
                StringBuilder method = new StringBuilder();
                method.append("@Override protected void onCreate(android.os.Bundle savedInstanceState) {\n");
                method.append("super.onCreate(savedInstanceState);\n");
                method.append("\t// TODO: add setContentView(...) invocation\n");
                method.append(preIOC.getCanonicalBindStatement());
                method.append("(this);\n");
                method.append("}");

                mClass.add(mFactory.createMethodFromText(method.toString(), mClass));
            } else {
                PsiMethod onCreate = mClass.findMethodsByName("onCreate", false)[0];
                if (!containsButterKnifeInjectLine(onCreate, preIOC.getSimpleBindStatement())) {
                    for (PsiStatement statement : onCreate.getBody().getStatements()) {
                        // Search for setContentView()
                        if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                            PsiReferenceExpression methodExpression
                                    = ((PsiMethodCallExpression) statement.getFirstChild())
                                    .getMethodExpression();
                            if (methodExpression.getText().equals("setContentView")) {
                                onCreate.getBody().addAfter(mFactory.createStatementFromText(
                                        preIOC.getCanonicalBindStatement() + "(this);", mClass), statement);
                                break;
                            }
                        }
                    }
                }
            }
            // Check for Fragment class
        } else if ((fragmentClass != null && mClass.isInheritor(fragmentClass, true)) || (supportFragmentClass != null && mClass.isInheritor(supportFragmentClass, true))) {
            if (mClass.findMethodsByName("onCreateView", false).length == 0) {
                // Add an empty stub of onCreateView()
                StringBuilder method = new StringBuilder();
                method.append("@Override public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, android.os.Bundle savedInstanceState) {\n");
                method.append("\t// TODO: inflate a fragment view\n");
                method.append("android.view.View rootView = super.onCreateView(inflater, container, savedInstanceState);\n");
                method.append(preIOC.getCanonicalBindStatement());
                method.append("(this, rootView);\n");
                method.append("return rootView;\n");
                method.append("}");

                mClass.add(mFactory.createMethodFromText(method.toString(), mClass));
            } else {
                PsiMethod onCreateView = mClass.findMethodsByName("onCreateView", false)[0];
                if (!containsButterKnifeInjectLine(onCreateView, preIOC.getSimpleBindStatement())) {
                    for (PsiStatement statement : onCreateView.getBody().getStatements()) {
                        if (statement instanceof PsiReturnStatement) {
                            String returnValue = ((PsiReturnStatement) statement).getReturnValue().getText();
                            if (returnValue.contains("R.layout")) {
                                onCreateView.getBody().addBefore(mFactory.createStatementFromText("android.view.View view = " + returnValue + ";", mClass), statement);
                                onCreateView.getBody().addBefore(mFactory.createStatementFromText(preIOC.getCanonicalBindStatement() + "(this, view);", mClass), statement);
                                statement.replace(mFactory.createStatementFromText("return view;", mClass));
                            } else {
                                onCreateView.getBody().addBefore(mFactory.createStatementFromText(
                                        preIOC.getCanonicalBindStatement() + "(this, " + returnValue + ");", mClass), statement);
                            }
                            break;
                        }
                    }
                }
            }

            // Create onDestroyView method if it's missing
            if (mClass.findMethodsByName("onDestroyView", false).length == 0) {
                StringBuilder method = new StringBuilder();
                method.append("@Override public void onDestroyView() {\n");
                method.append("super.onDestroyView();\n");
                method.append(preIOC.getCanonicalUnbindStatement());
                method.append("(this);\n");
                method.append("}");

                mClass.add(mFactory.createMethodFromText(method.toString(), mClass));
            } else {
                PsiMethod onDestroyView = mClass.findMethodsByName("onDestroyView", false)[0];
                if (!containsButterKnifeInjectLine(onDestroyView, preIOC.getSimpleUnbindStatement())) {
                    onDestroyView.getBody().addBefore(
                            mFactory.createStatementFromText(preIOC.getCanonicalUnbindStatement() + "(this);",
                                    mClass), onDestroyView.getBody().getLastBodyElement());
                }
            }
        }
    }
}
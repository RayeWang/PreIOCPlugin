package wang.raye.android.preiocplugin.preioc;

import java.util.regex.Pattern;

/**
 * @author Tomáš Kypta
 * @since 1.3
 */
public abstract class AbstractPreIOC implements IPreIOC {

    private static final String mPackageName = "wang.raye.preioc.annotation";
    private final Pattern mFieldAnnotationPattern = Pattern.compile("^@" + getFieldAnnotationSimpleName() + "\\(([^\\)]+)\\)$", Pattern.CASE_INSENSITIVE);
    private final String mFieldAnnotationCanonicalName = getPackageName() + "." + getFieldAnnotationSimpleName();
    private final String mCanonicalBindStatement =   getSimpleBindStatement();
    private final String mCanonicalUnbindStatement =  getSimpleUnbindStatement();
    private final String mOnClickCanonicalName =  "OnClick";


    @Override
    public Pattern getFieldAnnotationPattern() {
        return mFieldAnnotationPattern;
    }

    @Override
    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public String getFieldAnnotationCanonicalName() {
        return mFieldAnnotationCanonicalName;
    }

    @Override
    public String getOnClickAnnotationCanonicalName() {
        return mOnClickCanonicalName;
    }

    @Override
    public String getCanonicalBindStatement() {
        return mCanonicalBindStatement;
    }

    @Override
    public String getCanonicalUnbindStatement() {
        return mCanonicalUnbindStatement;
    }
}

package wang.raye.android.preiocplugin.preioc;

/**
 * ButterKnife version 6
 *
 * @author Tomáš Kypta
 * @since 1.3
 */
public class PreIOC106 extends AbstractPreIOC {

    private static final String mFieldAnnotationSimpleName = "BindById";
    private static final String mSimpleBindStatement = "PreIOC.binder";
    private static final String mSimpleUnbindStatement = "PreIOC.unBinder";


    @Override
    public String getVersion() {
        return "1.0.6";
    }

    @Override
    public String getDistinctClassName() {
        return getFieldAnnotationCanonicalName();
    }

    @Override
    public String getFieldAnnotationSimpleName() {
        return mFieldAnnotationSimpleName;
    }

    @Override
    public String getSimpleBindStatement() {
        return mSimpleBindStatement;
    }

    @Override
    public String getSimpleUnbindStatement() {
        return mSimpleUnbindStatement;
    }
}

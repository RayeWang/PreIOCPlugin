package wang.raye.android.preiocplugin.iface;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import wang.raye.android.preiocplugin.model.Element;

import java.util.ArrayList;

public interface IConfirmListener {

    public void onConfirm(Project project, Editor editor, ArrayList<Element> elements, String fieldNamePrefix, boolean createHolder);
}

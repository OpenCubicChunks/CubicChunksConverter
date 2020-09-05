package cubicchunks.converter.lib.conf.command;

import cubicchunks.converter.lib.util.EditTask;

import java.util.ArrayList;
import java.util.List;

public class EditTaskContext {

    private final List<EditTask> tasks = new ArrayList<>();

    public void addEditTask(EditTask task) {
        this.tasks.add(task);
    }
    public List<EditTask> getTasks() {
        return tasks;
    }
}

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MultiSelectComboBox extends JComboBox<Object> {
    private final List<String> selectedItems = new ArrayList<>();
    private final DefaultComboBoxModel<Object> model;

    public MultiSelectComboBox(List<String> items) {
        model = new DefaultComboBoxModel<>();
        model.addElement("All"); // Default option to send to all
        for (String item : items) {
            model.addElement(item);
        }
        setModel(model);

        setRenderer(new CheckBoxRenderer());
        addActionListener(e -> {
            if (getSelectedIndex() == 0) { // "All" selected
                selectedItems.clear();
                selectedItems.add("All");
            } else {
                Object selected = getSelectedItem();
                if (selectedItems.contains(selected)) {
                    selectedItems.remove(selected); // Uncheck
                } else {
                    selectedItems.add((String) selected); // Check
                }
            }
            updateModel();
        });
    }

    private void updateModel() {
        // Update the model to reflect the selected state
        model.removeAllElements();
        model.addElement("All");
        for (String item : selectedItems) {
            model.addElement(item);
        }
        repaint();
    }

    public List<String> getSelectedItems() {
        return selectedItems;
    }

    private class CheckBoxRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JCheckBox checkBox = new JCheckBox(value.toString());
            checkBox.setOpaque(true);
            checkBox.setSelected(selectedItems.contains(value));
            checkBox.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
            checkBox.setForeground(Color.BLACK);
            return checkBox;
        }
    }
}
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Export implements BurpExtension {
    private MontoyaApi api;
    private MyTableModel tableModel;
    private JTextField filterField;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Export");

        tableModel = new MyTableModel();
        api.userInterface().registerSuiteTab("Export", constructLoggerTab(tableModel));
        api.http().registerHttpHandler(new MyHttpHandler(tableModel));
    }

    private Component constructLoggerTab(MyTableModel tableModel) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        filterField = new JTextField();
        filterField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void textChanged() {
                tableModel.setFilter(filterField.getText());
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        UserInterface userInterface = api.userInterface();

        HttpRequestEditor requestViewer = userInterface.createHttpRequestEditor(EditorOptions.READ_ONLY);
        HttpResponseEditor responseViewer = userInterface.createHttpResponseEditor(EditorOptions.READ_ONLY);

        tabs.addTab("Request", requestViewer.uiComponent());
        tabs.addTab("Response", responseViewer.uiComponent());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterField, BorderLayout.CENTER);
        // 在 topPanel 创建后添加复制按钮
        JButton copySelectedButton = new JButton("复制选中项");
        copySelectedButton.addActionListener(e -> copySelectedRequests(tableModel, requestViewer, responseViewer));

// 将按钮添加到 topPanel 的右侧
        topPanel.add(copySelectedButton, BorderLayout.EAST);

        splitPane.setRightComponent(tabs);

        // 创建表格并启用排序
        JTable table = new JTable(tableModel) {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                // 将视图索引转换为模型索引
                int modelRowIndex = convertRowIndexToModel(rowIndex);
                HttpResponseReceived responseReceived = tableModel.get(modelRowIndex);
                if (responseReceived != null) {
                    requestViewer.setRequest(responseReceived.initiatingRequest());
                    responseViewer.setResponse(responseReceived);
                }
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        // 设置复选框列的渲染器和编辑器
        table.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(value != null && (Boolean) value);
                checkBox.setHorizontalAlignment(JLabel.CENTER);
                return checkBox;
            }
        });

        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));

        // 启用表格排序功能
        TableRowSorter<MyTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // 添加列标题点击事件处理
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = table.getColumnModel().getColumnIndexAtX(e.getX());
                if (column > 0) { // 只对非复选框列进行排序
                    tableModel.sort(column - 1); // 调整列索引（因为新增了第一列）
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(mainPanel);

        return splitPane;
    }

    private void copySelectedRequests(MyTableModel tableModel, HttpRequestEditor requestViewer, HttpResponseEditor responseViewer) {
        List<HttpResponseReceived> selectedResponses = new ArrayList<>();

        // 获取所有选中的行
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0) instanceof Boolean && (Boolean) tableModel.getValueAt(i, 0)) {
                HttpResponseReceived response = tableModel.get(i);
                if (response != null) {
                    selectedResponses.add(response);
                }
            }
        }

        if (selectedResponses.isEmpty()) {
            api.logging().logToOutput("没有选中任何项");
            return;
        }

        try {
            // 构建复制内容
            StringBuilder clipboardContent = new StringBuilder();
            for (int i = 0; i < selectedResponses.size(); i++) {
                HttpResponseReceived response = selectedResponses.get(i);
                String requestStr = response.initiatingRequest().toByteArray().toString();
                String responseStr = response.toByteArray().toString();

                clipboardContent.append("请求包").append(i + 1).append(": ").append(requestStr).append("\n");
                clipboardContent.append("返回包").append(i + 1).append(": ").append(responseStr).append("\n\n");
            }

            // 复制到剪贴板
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(clipboardContent.toString());
            clipboard.setContents(selection, null);

            api.logging().logToOutput("已复制 " + selectedResponses.size() + " 个选中项到剪贴板");
        } catch (Exception e) {
            api.logging().logToError("复制失败: " + e.getMessage());
        }
    }




}

// Helper class for document listener
abstract class SimpleDocumentListener implements DocumentListener {
    @Override
    public void insertUpdate(DocumentEvent e) { textChanged(); }

    @Override
    public void removeUpdate(DocumentEvent e) { textChanged(); }

    @Override
    public void changedUpdate(DocumentEvent e) { textChanged(); }

    public abstract void textChanged();
}

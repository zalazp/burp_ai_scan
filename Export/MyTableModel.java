import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.requests.HttpRequest;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class MyTableModel extends AbstractTableModel {
    private final List<HttpResponseReceived> log;
    private final List<HttpResponseReceived> filteredLog;
    private String filter = "";

    // 添加排序相关字段
    private int sortColumn = -1;
    private boolean ascending = true;

    // 添加选中状态列表
    private List<Boolean> selectedRows;

    public MyTableModel() {
        this.log = new ArrayList<>();
        this.filteredLog = new ArrayList<>();
        this.selectedRows = new ArrayList<>();
    }

    @Override
    public synchronized int getRowCount() {
        updateFilteredLog();
        return filteredLog.size();
    }

    @Override
    public int getColumnCount() {
        return 6; // 增加一列用于复选框
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "#"; // 复选框列
            case 1 -> "Tool"; // 新增的 Tool 列
            case 2 -> "URL";
            case 3 -> "Status code";
            case 4 -> "Method";
            case 5 -> "Length";
            default -> "";
        };
    }

    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        updateFilteredLog();
        if (rowIndex >= filteredLog.size()) return "";

        HttpResponseReceived responseReceived = filteredLog.get(rowIndex);

        // 处理复选框列
        if (columnIndex == 0) {
            // 确保 selectedRows 列表大小与 filteredLog 匹配
            while (selectedRows.size() <= rowIndex) {
                selectedRows.add(false);
            }
            return selectedRows.get(rowIndex);
        }

        // 处理 Tool 列
        if (columnIndex == 1) {
            return responseReceived.toolSource().toolType().toString(); // 获取工具类型
        }

        // 其他列保持不变
        return switch (columnIndex) {
            case 2 -> responseReceived.initiatingRequest().url();
            case 3 -> responseReceived.statusCode();
            case 4 -> responseReceived.initiatingRequest().method();
            case 5 -> responseReceived.body().length();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) { // 复选框列
            return Boolean.class;
        }
        return super.getColumnClass(columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0; // 只有复选框列可编辑
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && aValue instanceof Boolean) {
            // 确保 selectedRows 列表大小与 filteredLog 匹配
            while (selectedRows.size() <= rowIndex) {
                selectedRows.add(false);
            }
            selectedRows.set(rowIndex, (Boolean) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public synchronized void add(HttpResponseReceived responseReceived) {
//        if ("Proxy".equals(responseReceived.toolSource().toolType().toString())) {
            int index = log.size();
            log.add(responseReceived);
            updateFilteredLog(); // 更新过滤日志以保持同步
            fireTableRowsInserted(index, index);

    }

    public synchronized HttpResponseReceived get(int rowIndex) {
        updateFilteredLog();
        if (rowIndex >= filteredLog.size()) return null;
        return filteredLog.get(rowIndex);
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilteredLog();
        fireTableDataChanged();
    }

    private void updateFilteredLog() {
        filteredLog.clear();
        List<HttpResponseReceived> tempLog = log.stream()
                .filter(response -> response.initiatingRequest().url().toString().contains(filter))
                .collect(Collectors.toList());

        // 应用排序
        if (sortColumn >= 0) {
            tempLog.sort(getComparator());
            if (!ascending) {
                Collections.reverse(tempLog);
            }
        }
        filteredLog.addAll(tempLog);

        // 同步 selectedRows 列表大小
        while (selectedRows.size() < filteredLog.size()) {
            selectedRows.add(false);
        }
        // 如果 filteredLog 缩小，移除多余的 selectedRows
        while (selectedRows.size() > filteredLog.size()) {
            selectedRows.remove(selectedRows.size() - 1);
        }
    }

    // 排序功能
    public void sort(int column) {
        if (sortColumn == column) {
            // 如果是同一列，切换升序/降序
            ascending = !ascending;
        } else {
            // 新列，设为升序
            sortColumn = column;
            ascending = true;
        }
        updateFilteredLog();
        fireTableDataChanged();
    }

    // 修改排序功能：添加对 Tool 列的排序支持
    private Comparator<HttpResponseReceived> getComparator() {
        return (r1, r2) -> {
            switch (sortColumn) {
                case 1: // Tool
                    return r1.toolSource().toolType().toString()
                            .compareToIgnoreCase(r2.toolSource().toolType().toString());
                case 2: // URL
                    return r1.initiatingRequest().url().toString()
                            .compareToIgnoreCase(r2.initiatingRequest().url().toString());
                case 3: // Status code
                    return Integer.compare(r1.statusCode(), r2.statusCode());
                case 4: // Method
                    return r1.initiatingRequest().method()
                            .compareToIgnoreCase(r2.initiatingRequest().method());
                case 5: // Length
                    return Integer.compare(r1.body().length(), r2.body().length());
                default:
                    return 0;
            }
        };
    }

    public List<HttpResponseReceived> getFiltered() {
        return new ArrayList<>(filteredLog);
    }

    // 用于外部获取排序状态
    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isAscending() {
        return ascending;
    }
}

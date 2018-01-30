package pers.quzhe.devicemanager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.sun.jna.NativeLong;

import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import net.miginfocom.swing.MigLayout;
import pers.quzhe.UI.DeviceUI;
import pers.quzhe.bean.DeviceInfo;
import pers.quzhe.hcnetsdk.HCNetSDK;
import pers.quzhe.util.JdbcUtil;

/**
 * <p>
 * Title: DeviceList<／p>
 * <p>
 * Description: <／p>
 * 
 * @author quzhe
 * 
 *         2017年11月20日
 */
public class DeviceList{
	/**
	 * 
	 */
	private static final long serialVersionUID = -311019214878889971L;

	// rowData用来存放行数据
	// columnNames存放列名
	Vector<String> columnNames;
	Vector rowData = new Vector();

	// 设备属性
	public String deviceId;
	public String type;
	public String model;
	public String ip;
	public int port;
	public String userName;
	public String password;
	public String language;
	public String comment;
	public String position;
	public boolean status;

	private static JFrame frame;// 单例模式
	private static JPanel panel_center = new JPanel();
	private JPanel panel_north;
	private JTable jt = null;
	private JTable jt_checked = null; // 用来存放筛选后的表格
	private JScrollPane jsp = null;
	private JButton btn_check;
	private JButton btnExcel;
	private JLabel label;
	private JComboBox comboBox;
	private static boolean ONLINE_FLAG;
	private DefaultTableModel tableModel;
	private DefaultTableModel tableModel_checked;
	private TableRowSorter sorter;
	private JdbcUtil jdbcUtil = new JdbcUtil();

	NativeLong lUserID;// 用户句柄
	static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
	HCNetSDK.NET_DVR_DEVICEINFO m_strDeviceInfo;// 设备信息

	String index = null;
	String sql = null;
	public List<Map<String, Object>> mapList = null;
	
	int index_1;

	public DeviceList(String s) {

		boolean initSuc = hCNetSDK.NET_DVR_Init();
		if (initSuc != true) {
			JOptionPane.showMessageDialog(null, "初始化失败");
		}

		lUserID = new NativeLong(-1);
		if (frame == null) {
			frame = new JFrame();
		}
		frame.setBounds(100, 347, 1550, 410);
		// frame.setSize(1500, 300);
		frame.setIconImage(
				Toolkit.getDefaultToolkit().getImage(DeviceList.class.getResource("/pers/quzhe/res/device_1.png")));
		frame.setTitle("\u8BBE\u5907\u5217\u8868");
		frame.setLayout(new BorderLayout(0, 0));

		ONLINE_FLAG = true;
		columnNames = new Vector();
		// 设置列名
		columnNames.add("状态");
		columnNames.add("设备ID");
		columnNames.add("设备类型");
		columnNames.add("设备型号");
		columnNames.add("IP");
		columnNames.add("端口号");
		columnNames.add("用户名");
		columnNames.add("密码");
		columnNames.add("语言");
		columnNames.add("位置");
		columnNames.add("备注");
		columnNames.add("");// 修改设备信息按钮(update)
		columnNames.add("");// 删除设备信息按钮(delete)

		panel_north = new JPanel();
		frame.add(panel_north, BorderLayout.NORTH);
		panel_north.setLayout(new MigLayout("", "[62px][62px][57px][136px][136px][][]", "[23px]"));

		label = new JLabel("\u5728\u7EBF\u72B6\u6001\uFF1A");
		panel_north.add(label, "cell 0 0,grow");

		comboBox = new JComboBox();
		comboBox.setModel(new DefaultComboBoxModel(new String[] { "\u5728\u7EBF", "\u4E0D\u5728\u7EBF" }));
		panel_north.add(comboBox, "cell 1 0,alignx left,growy");
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (comboBox.getSelectedIndex() == 0) {
					ONLINE_FLAG = true;
				} else if (comboBox.getSelectedIndex() == 1) {
					ONLINE_FLAG = false;
				}
			}
		});

		btn_check = new JButton("\u7B5B\u9009");
		panel_north.add(btn_check, "cell 2 0,alignx left,aligny top");
		btn_check.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				jt.setVisible(false);
				sorter = new TableRowSorter(tableModel);
				if (ONLINE_FLAG) {
					sorter.setRowFilter(RowFilter.regexFilter("#ONLINE#"));
				} else {
					sorter.setRowFilter(RowFilter.regexFilter("#OFFLINE#"));
				}
				jt.setRowSorter(sorter); // 为JTable设置排序器

				int jR = jt.getRowCount();
				int jC = jt.getColumnCount();
				Object[][] obj = new Object[jR][jC];
				for (int ir = 0; ir < jR; ir++) {
					for (int ic = 0; ic < jC - 2; ic++) {
						obj[ir][ic] = jt.getValueAt(ir, ic);
					}
				}
				String[] name = { "状态", "设备ID", "设备类型", "设备型号", "IP", "端口号", "用户名", "密码", "语言", "位置", "备注" };
				tableModel_checked = new DefaultTableModel(obj, name);
				jt_checked = new JTable(tableModel_checked);

				// 设置表格渲染器
				jt_checked.getColumnModel().getColumn(0).setCellRenderer(new MyRenderStatus());

				// 初始化 jsp
				jt_checked.setVisible(true);
				jsp.setViewportView(jt_checked);
			}
		});

		btnExcel = new JButton("\u5BFC\u51FAExcel");
		panel_north.add(btnExcel, "cell 4 0,growx,aligny top");
		btnExcel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String excel_Url = "..\\SDKAuto\\Generate Files\\Excel\\Device_" + System.currentTimeMillis() + ".xls";
				File file = new File(excel_Url);
				try {
					if (jt_checked != null) {
						exportTable(jt_checked, file);
					} else {
						exportTable(jt, file);
					}
					JOptionPane.showMessageDialog(null, "导出成功");
				} catch (IOException e1) {
				}
			}
		});
		// 创建填充参数的list
				List<Object> paramList = new ArrayList<Object>();
				switch (DeviceSelect.index) {
				// byType
				case 1:
					index = s;
					sql = "select * from device where type = ?";
					// 填充参数
					paramList.add(index);
					break;
				// byModel
				case 2:
					index = "%" + s + "%";
					sql = "select * from device where model like ?";
					// 填充参数
					paramList.add(index);
					break;
				// byIp
				case 3:
					index = s;
					sql = "select * from device where ip = ?";
					// 填充参数
					paramList.add(index);
					break;
				// byComment
				case 4:
					index = "%" + s + "%";
					sql = "select * from device where comment like ?";
					// 填充参数
					paramList.add(index);
					break;
				// selectAll
				case 5:
					sql = "select * from device";
					break;
				}

				try {
					jdbcUtil.getConnection(); // 获取数据库链接
					mapList = jdbcUtil.findResult(sql.toString(), paramList);
				} catch (SQLException exception) {
					System.out.println(this.getClass() + "执行查询操作抛出异常！");
					exception.printStackTrace();
				} finally {
					if (jdbcUtil != null) {
						jdbcUtil.releaseConn();
					}
				}
	}

	public void show() {

		// 修改
		MyEvent eventModify = new MyEvent() {
			@Override
			public void invoke(ActionEvent e) {
				MyButton button = (MyButton) e.getSource();
				int row = button.getRow();
				deviceId = (String) jt.getValueAt(row, 1);
				type = (String) jt.getValueAt(row, 2);
				model = (String) jt.getValueAt(row, 3);
				ip = (String) jt.getValueAt(row, 4);
				port = (int) jt.getValueAt(row, 5);
				userName = (String) jt.getValueAt(row, 6);
				password = (String) jt.getValueAt(row, 7);
				language = (String) jt.getValueAt(row, 8);
				position = (String) jt.getValueAt(row, 9);
				comment = (String) jt.getValueAt(row, 10);

				String sql = "update device set deviceid=?,type=?,model=?,ip=?,port=?,username=?,password=?,language=?,position=?,comment=? where deviceid=?";
				// 创建填充参数的list
				List<Object> paramList = new ArrayList<Object>();
				// 填充参数
				paramList.add(deviceId);
				paramList.add(type);
				paramList.add(model);
				paramList.add(ip);
				paramList.add(port);
				paramList.add(userName);
				paramList.add(password);
				paramList.add(language);
				paramList.add(position);
				paramList.add(comment);
				paramList.add(deviceId);

				boolean bool = false;
				try {
					jdbcUtil.getConnection(); // 获取数据库链接
					bool = jdbcUtil.updateByPreparedStatement(sql, paramList);
				} catch (SQLException e1) {
					System.out.println(this.getClass() + "执行修改操作抛出异常！");
					e1.printStackTrace();
				} finally {
					if (jdbcUtil != null) {
						jdbcUtil.releaseConn();
					}
				}
				System.out.println("执行修改的结果：" + bool);
				if (bool) {
					JOptionPane.showMessageDialog(null, "修改成功");
				} else {
					JOptionPane.showMessageDialog(null, "修改失败", "错误", JOptionPane.ERROR_MESSAGE);
				}
			}

		};

		// 删除
		MyEvent eventDelete = new MyEvent() {
			@Override
			public void invoke(ActionEvent e) {
				MyButton button = (MyButton) e.getSource();
				int row = button.getRow();
				deviceId = (String) jt.getValueAt(row, 1);

				String sql = "delete from device where deviceid=?";
				// 创建填充参数的list
				List<Object> paramList = new ArrayList<Object>();
				// 填充参数
				paramList.add(deviceId);

				boolean bool = false;
				try {
					jdbcUtil.getConnection(); // 获取数据库链接
					bool = jdbcUtil.updateByPreparedStatement(sql, paramList);
				} catch (SQLException exception) {
					System.out.println(this.getClass() + "执行删除操作抛出异常！");
					exception.printStackTrace();
				} finally {
					if (jdbcUtil != null) {
						jdbcUtil.releaseConn();
					}
				}
				System.out.println("执行删除的结果：" + bool);
				if (bool) {
					JOptionPane.showMessageDialog(null, "删除成功");
				} else {
					JOptionPane.showMessageDialog(null, "删除失败", "错误", JOptionPane.ERROR_MESSAGE);
				}
			}

		};

//		// 创建填充参数的list
//		List<Object> paramList = new ArrayList<Object>();
//		switch (DeviceSelect.index) {
//		// byType
//		case 1:
//			index = s;
//			sql = "select * from device where type = ?";
//			// 填充参数
//			paramList.add(index);
//			break;
//		// byModel
//		case 2:
//			index = "%" + s + "%";
//			sql = "select * from device where model like ?";
//			// 填充参数
//			paramList.add(index);
//			break;
//		// byIp
//		case 3:
//			index = s;
//			sql = "select * from device where ip = ?";
//			// 填充参数
//			paramList.add(index);
//			break;
//		// byComment
//		case 4:
//			index = "%" + s + "%";
//			sql = "select * from device where comment like ?";
//			// 填充参数
//			paramList.add(index);
//			break;
//		// selectAll
//		case 5:
//			sql = "select * from device";
//			break;
//		}
//
//		try {
//			jdbcUtil.getConnection(); // 获取数据库链接
//			mapList = jdbcUtil.findResult(sql.toString(), paramList);
//		} catch (SQLException exception) {
//			System.out.println(this.getClass() + "执行查询操作抛出异常！");
//			exception.printStackTrace();
//		} finally {
//			if (jdbcUtil != null) {
//				jdbcUtil.releaseConn();
//			}
//		}
//		for (index_1 = 0; index_1 < mapList.size(); index_1++) {
//			Map<String, Object> map = mapList.get(index_1);
//			try {
//				// 设备属性
//				deviceId = (String) map.get("deviceid");
//				type = (String) map.get("type");
//				model = (String) map.get("model");
//				ip = (String) map.get("ip");
//				port = (int) map.get("port");
//				userName = (String) map.get("username");
//				password = (String) map.get("password");
//				language = (String) map.get("language");
//				position = (String) map.get("position");
//				comment = (String) map.get("comment");
//			} catch (Exception exception) {
//			}
			// m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO();
			// lUserID = hCNetSDK.NET_DVR_Login(ip, (short) port, userName,
			// password, m_strDeviceInfo);
			// status = (lUserID.intValue() == -1) ? false : true;
//			new Thread(new DeviceList(ip, port, userName, password, index_1)).start();
			// if (status) {
			// online.add(index_1);
			// } else {
			// offline.add(index_1);
			// }
			//
//			Vector hang = new Vector();
//			if (status) {
//				hang.add("#ONLINE#");
//			} else {
//				hang.add("#OFFLINE#");
//			}
//			hang.add(deviceId);
//			hang.add(type);
//			hang.add(model);
//			hang.add(ip);
//			hang.add(port);
//			hang.add(userName);
//			hang.add(password);
//			hang.add(language);
//			hang.add(position);
//			hang.add(comment);
//
//			rowData.add(hang);
//		}
		for(DeviceInfo d : DeviceSelect.vectors_device){
//			Vector hang = new Vector();
//			if (d.getStatus()) {
//				hang.add("#ONLINE#");
//			} else {
//				hang.add("#OFFLINE#");
//			}
//			hang.add(d.getDeviceId());
//			hang.add(d.getType());
//			hang.add(d.getModel());
//			hang.add(d.getIp());
//			hang.add(d.getPort());
//			hang.add(d.getUserName());
//			hang.add(d.getPassword());
//			hang.add(d.getLanguage());
//			hang.add(d.getPosition());
//			hang.add(d.getComment());

			rowData.add(d.getHang());
		}
		tableModel = new DefaultTableModel(rowData, columnNames);
		jt = new JTable(tableModel);
		jt.setPreferredScrollableViewportSize(new Dimension(1500, 300));

		// 设置表格渲染器
		jt.getColumnModel().getColumn(0).setCellRenderer(new MyRenderStatus());
		// 设置表格渲染器
		jt.getColumnModel().getColumn(11).setCellRenderer(new MyRenderModify());
		// 设置表格的编辑器
		jt.getColumnModel().getColumn(11).setCellEditor(new MyRenderModify(eventModify));

		// 设置表格渲染器
		jt.getColumnModel().getColumn(12).setCellRenderer(new MyRenderDelete());
		// 设置表格的编辑器
		jt.getColumnModel().getColumn(12).setCellEditor(new MyRenderDelete(eventDelete));

		// 初始化 jsp
		jsp = new JScrollPane(jt);
		jsp.setSize(1400, 300);
		jsp.setViewportView(jt);
		// 把jsp放入到jpanel

		panel_center.removeAll();
		panel_center.add(jsp, BorderLayout.CENTER);
		panel_center.repaint();
		// this.setSize(1500, 300);
		frame.add(panel_center);

		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	class MyRenderModify extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

		private MyButton button;

		private MyEvent event;

		public MyRenderModify() {
			button = new MyButton("修改");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int res = JOptionPane.showConfirmDialog(null, "确认修改该设备？", "是否继续", JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.YES_OPTION) {
						event.invoke(e);
					} else {
						return;
					}

				}

			});

		}

		public MyRenderModify(MyEvent e) {
			this();
			this.event = e;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			// TODO Auto-generated method stub
			// 单元格提示
			// button.setToolTipText("");
			// 背景色
			button.setBackground(Color.BLACK);
			// 前景色
			button.setForeground(Color.green);
			return button;
		}

		/*
		 * 重写编辑器方法，返回一个按钮给JTable
		 */
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			// setClickCountToStart(1);
			// 将这个被点击的按钮所在的行和列放进button里面

			button.setRow(row);
			button.setColumn(column);
			// 背景色
			button.setBackground(Color.BLACK);
			// 前景色
			button.setForeground(Color.green);
			return button;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		@Override
		public Object getCellEditorValue() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class MyRenderDelete extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

		private MyButton button;

		private MyEvent event;

		public MyRenderDelete() {
			button = new MyButton("删除");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int res = JOptionPane.showConfirmDialog(null, "确认删除该设备？", "是否继续", JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.YES_OPTION) {
						event.invoke(e);
					} else {
						return;
					}
				}

			});

		}

		public MyRenderDelete(MyEvent e) {

			this();
			this.event = e;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			// TODO Auto-generated method stub
			// 单元格提示
			// button.setToolTipText("");
			// 背景色
			button.setBackground(Color.BLACK);
			// 前景色
			button.setForeground(Color.red);
			return button;
		}

		/*
		 * 重写编辑器方法，返回一个按钮给JTable
		 */
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			// setClickCountToStart(1);
			// 将这个被点击的按钮所在的行和列放进button里面
			button.setRow(row);
			button.setColumn(column);
			// 背景色
			button.setBackground(Color.BLACK);
			// 前景色
			button.setForeground(Color.red);
			return button;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		@Override
		public Object getCellEditorValue() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class MyRenderStatus implements TableCellRenderer {
		private JLabel jl;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			// TODO Auto-generated method stub
			if (value.toString().equals("#ONLINE#")) {
				jl = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit()
						.getImage(DeviceUI.class.getResource("/pers/quzhe/res/online.jpg"))));
			} else {
				jl = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit()
						.getImage(DeviceUI.class.getResource("/pers/quzhe/res/offline.jpg"))));
			}
			return jl;
		}
	}

//	class MyRenderStatus1 implements TableCellRenderer {
//		private JLabel jl;
//
//		@Override
//		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
//				int row, int column) {
//			// TODO Auto-generated method stub
//			if (online.contains(row)) {
//				jl = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit()
//						.getImage(DeviceUI.class.getResource("/pers/quzhe/res/online.jpg"))));
//			} else {
//				jl = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit()
//						.getImage(DeviceUI.class.getResource("/pers/quzhe/res/offline.jpg"))));
//			}
//			return jl;
//		}
//	}

	class MyButton extends JButton {

		private int row;

		private int column;

		public int getRow() {
			return row;
		}

		public void setRow(int row) {
			this.row = row;
		}

		public int getColumn() {
			return column;
		}

		public void setColumn(int column) {
			this.column = column;
		}

		public MyButton() {

		}

		public MyButton(String name) {
			super(name);
		}

	}

	class MyLabel extends JLabel {

		private int row;

		private int column;

		public int getRow() {
			return row;
		}

		public void setRow(int row) {
			this.row = row;
		}

		public int getColumn() {
			return column;
		}

		public void setColumn(int column) {
			this.column = column;
		}

		public MyLabel() {

		}

		public MyLabel(String name) {
			super(name);
		}

	}

	abstract class MyEvent {
		public abstract void invoke(ActionEvent e);
	}

	/**
	 * 导出jtable的model到excel
	 * 
	 * @param table
	 *            要导出的jtable
	 * @param file
	 *            要导出到的file
	 * @throws IOException
	 *             IO异常
	 */
	public static void exportTable(JTable table, File file) throws IOException {
		try {
			OutputStream out = new FileOutputStream(file);
			TableModel model = table.getModel();
			WritableWorkbook wwb = Workbook.createWorkbook(out);
			// 创建字表，并写入数据
			WritableSheet ws = wwb.createSheet("DeviceInfo", 0);
			// 添加标题
			for (int i = 0; i < model.getColumnCount() - 1; i++) {
				jxl.write.Label labelN = new jxl.write.Label(i, 0, model.getColumnName(i + 1));
				try {
					ws.addCell(labelN);
				} catch (RowsExceededException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (WriteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// 添加列
			for (int i = 0; i < model.getColumnCount() - 1; i++) {
				for (int j = 0; j < model.getRowCount(); j++) {
					try {
						jxl.write.Label labelN = new jxl.write.Label(i, j + 1, model.getValueAt(j, i + 1).toString());
						ws.addCell(labelN);
					} catch (RowsExceededException e) {
						e.printStackTrace();
					} catch (WriteException e) {
						e.printStackTrace();
					} catch (NullPointerException e) {
					}
				}
			}
			wwb.write();
			try {
				wwb.close();
			} catch (WriteException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "导入数据前请关闭工作表");
		}
	}
}
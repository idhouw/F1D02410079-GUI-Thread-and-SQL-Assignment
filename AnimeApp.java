import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnimeApp extends JFrame {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/db_anime";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private final JTextField tfJudul = new JTextField();
    private final JComboBox<String> cbGenre = new JComboBox<>(new String[]{
            "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror",
            "Romance", "Sci-Fi", "Slice of Life"});
    private final JSpinner spRating = new JSpinner(new SpinnerNumberModel(7.0, 0.0, 10.0, 0.1));
    private final JTextField tfCari = new JTextField(18);
    private final JLabel lblStatus = new JLabel("Siap");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Judul", "Genre", "Rating"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    private int selectedId = -1; 

    @FunctionalInterface
    private interface DbTask {
        void run(Connection con) throws SQLException;
    }

    public AnimeApp() {
        super("Daftar Anime");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 520);
        setLocationRelativeTo(null);
        buildUI();
        loadData();
    }

    private void buildUI() {
        cbGenre.setEditable(true);
        spRating.setEditor(new JSpinner.NumberEditor(spRating, "0.0"));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Judul"));
        form.add(tfJudul);
        form.add(new JLabel("Genre"));
        form.add(cbGenre);
        form.add(new JLabel("Rating (0 - 10)"));
        form.add(spRating);

        JButton btnTambah = new JButton("Tambah");
        JButton btnUbah = new JButton("Ubah");
        JButton btnHapus = new JButton("Hapus");
        JButton btnBersih = new JButton("Bersihkan");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(btnTambah);
        buttons.add(btnUbah);
        buttons.add(btnHapus);
        buttons.add(btnBersih);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBorder(new EmptyBorder(12, 12, 6, 12));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JButton btnCari = new JButton("Cari");
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        search.add(new JLabel("Cari judul/genre:"));
        search.add(tfCari);
        search.add(btnCari);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(0, 12, 0, 12));
        center.add(search, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        lblStatus.setBorder(new EmptyBorder(6, 12, 6, 12));

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);

        btnTambah.addActionListener(e -> tambah());
        btnUbah.addActionListener(e -> ubah());
        btnHapus.addActionListener(e -> hapus());
        btnBersih.addActionListener(e -> bersihkan());
        btnCari.addActionListener(e -> loadData());
        tfCari.addActionListener(e -> loadData());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) isiFormDariTabel();
        });
    }

    private void loadData() {
        String kw = "%" + tfCari.getText().trim() + "%";
        List<Object[]> rows = new ArrayList<>();

        runDb("Memuat data...", con -> {
            String sql = "SELECT id, judul, genre, rating FROM anime "
                    + "WHERE judul LIKE ? OR genre LIKE ? ORDER BY id DESC";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, kw);
                ps.setString(2, kw);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[]{
                                rs.getInt("id"), rs.getString("judul"),
                                rs.getString("genre"), rs.getDouble("rating")});
                    }
                }
            }
        }, () -> {
            model.setRowCount(0);
            for (Object[] r : rows) model.addRow(r);
            lblStatus.setText(rows.size() + " data ditampilkan");
        });
    }

    private void tambah() {
        if (!valid()) return;
        String judul = tfJudul.getText().trim();
        String genre = String.valueOf(cbGenre.getSelectedItem()).trim();
        double rating = getRating();

        runDb("Menyimpan data...", con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO anime (judul, genre, rating) VALUES (?, ?, ?)")) {
                ps.setString(1, judul);
                ps.setString(2, genre);
                ps.setDouble(3, rating);
                ps.executeUpdate();
            }
        }, () -> {
            bersihkan();
            loadData();
        });
    }

    private void ubah() {
        if (selectedId < 0) {
            peringatan("Pilih dulu data di tabel yang mau diubah.");
            return;
        }
        if (!valid()) return;
        int id = selectedId;
        String judul = tfJudul.getText().trim();
        String genre = String.valueOf(cbGenre.getSelectedItem()).trim();
        double rating = getRating();

        runDb("Mengubah data...", con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE anime SET judul = ?, genre = ?, rating = ? WHERE id = ?")) {
                ps.setString(1, judul);
                ps.setString(2, genre);
                ps.setDouble(3, rating);
                ps.setInt(4, id);
                ps.executeUpdate();
            }
        }, () -> {
            bersihkan();
            loadData();
        });
    }

    private void hapus() {
        if (selectedId < 0) {
            peringatan("Pilih dulu data di tabel yang mau dihapus.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Hapus data yang dipilih?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        int id = selectedId;

        runDb("Menghapus data...", con -> {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM anime WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }, () -> {
            bersihkan();
            loadData();
        });
    }

    private void runDb(String pesan, DbTask task, Runnable onSuccess) {
        lblStatus.setText(pesan);

        Thread worker = new Thread(() -> {
            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                task.run(con);
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Berhasil");
                    if (onSuccess != null) onSuccess.run();
                });
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Gagal");
                    JOptionPane.showMessageDialog(this,
                            "Operasi database gagal:\n" + ex.getMessage()
                                    + "\n\nPastikan MySQL di XAMPP sudah Start dan database 'db_anime' sudah dibuat.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "db-worker");
        worker.start();
    }

    private void isiFormDariTabel() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        selectedId = (int) model.getValueAt(row, 0);
        tfJudul.setText(String.valueOf(model.getValueAt(row, 1)));
        cbGenre.setSelectedItem(model.getValueAt(row, 2));
        spRating.setValue(((Number) model.getValueAt(row, 3)).doubleValue());
    }

    private void bersihkan() {
        selectedId = -1;
        tfJudul.setText("");
        cbGenre.setSelectedIndex(0);
        spRating.setValue(7.0);
        table.clearSelection();
    }

    private boolean valid() {
        if (tfJudul.getText().trim().isEmpty()) {
            peringatan("Judul tidak boleh kosong.");
            return false;
        }
        Object g = cbGenre.getSelectedItem();
        if (g == null || g.toString().trim().isEmpty()) {
            peringatan("Genre tidak boleh kosong.");
            return false;
        }
        return true;
    }

    private double getRating() {
        try {
            spRating.commitEdit(); 
        } catch (java.text.ParseException ignored) {
        }
        double v = ((Number) spRating.getValue()).doubleValue();
        return Math.round(v * 10) / 10.0;
    }

    private void peringatan(String pesan) {
        JOptionPane.showMessageDialog(this, pesan, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new AnimeApp().setVisible(true));
    }
}
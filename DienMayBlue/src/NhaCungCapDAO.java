package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import entity.*;
public class NhaCungCapDAO extends CoSoDAO {
    public String goiYMaNhaCungCap() throws SQLException {
        return goiYMaTiepTheo("NhaCungCap", "MaNhaCungCap", "NCC", 3);
    }
    public String goiYMaSoThueNhaCungCap() throws SQLException {
        return goiYMaTiepTheo("NhaCungCap", "MaSoThue", "MST", 3);
    }
    public void themCongNoNhaCungCap(Connection c, String maNhaCungCap, String ref, String desc, BigDecimal amount) throws SQLException {
        capNhat(c, "UPDATE dbo.NhaCungCap SET CongNo=CongNo+? WHERE MaNhaCungCap=?", amount, maNhaCungCap);
    }
    public void giamCongNoNhaCungCap(Connection c, String maNhaCungCap, BigDecimal amount) throws SQLException {
        capNhat(c, "UPDATE dbo.NhaCungCap SET CongNo=CASE WHEN CongNo>=? THEN CongNo-? ELSE 0 END WHERE MaNhaCungCap=?", amount, amount, maNhaCungCap);
    }
    public void capNhatTrangThaiNhaCungCap(Connection c, String maNhaCungCap, String status) throws SQLException {
        capNhat(c, "UPDATE dbo.NhaCungCap SET TrangThai=? WHERE MaNhaCungCap=?", status, maNhaCungCap);
    }
    public List<Map<String, Object>> danhSachNhaCungCap() throws SQLException {
        return truyVan("SELECT MaNhaCungCap supplier_code,TenNhaCungCap supplier_name,SoDienThoai phone,DiaChi address,Email email,MaSoThue tax_code,TrangThai status,CongNo total_debt FROM dbo.NhaCungCap ORDER BY MaNhaCungCap");
    }
    public boolean nhaCungCapTonTai(Connection c, String maNhaCungCap) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.NhaCungCap WHERE MaNhaCungCap=? AND TrangThai=N'Hoạt động'", maNhaCungCap)).intValue() > 0;
    }
    public List<Map<String, Object>> timDanhSachNhaCungCap(String key) throws SQLException {
        return truyVan("SELECT MaNhaCungCap supplier_code,TenNhaCungCap supplier_name,SoDienThoai phone,DiaChi address,Email email,MaSoThue tax_code,TrangThai status,CongNo total_debt FROM dbo.NhaCungCap WHERE MaNhaCungCap LIKE ? OR TenNhaCungCap LIKE ? OR SoDienThoai LIKE ? OR Email LIKE ? ORDER BY MaNhaCungCap", key, key, key, key);
    }
    public NhaCungCap timNhaCungCapThucThe(String maNhaCungCap) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) { return timNhaCungCapThucThe(c, maNhaCungCap); }
    }
    public NhaCungCap timNhaCungCapThucThe(Connection c, String maNhaCungCap) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT MaNhaCungCap supplier_code,TenNhaCungCap supplier_name,SoDienThoai phone,DiaChi address,Email email,MaSoThue tax_code,TrangThai status,CongNo total_debt FROM dbo.NhaCungCap WHERE MaNhaCungCap=?", maNhaCungCap);
        if (d == null) return null;
        return new NhaCungCap(String.valueOf(d.get("supplier_code")), String.valueOf(d.get("supplier_name")), String.valueOf(d.get("phone")), d.get("address") == null ? null : String.valueOf(d.get("address")), d.get("email") == null ? null : String.valueOf(d.get("email")), String.valueOf(d.get("tax_code")), String.valueOf(d.get("status")), (BigDecimal) d.get("total_debt"));
    }
    public int demTrungMaNhaCungCap(Connection c, String code, String skip) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.NhaCungCap WHERE MaNhaCungCap=? AND (? IS NULL OR MaNhaCungCap<>?)", code, skip, skip)).intValue();
    }
    public int demTrungMaSoThueNhaCungCap(Connection c, String tax, String skip) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.NhaCungCap WHERE MaSoThue=? AND (? IS NULL OR MaNhaCungCap<>?)", tax, skip, skip)).intValue();
    }
    public int demTrungSoDienThoaiNhaCungCap(Connection c, String phone, String skip) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.NhaCungCap WHERE SoDienThoai=? AND (? IS NULL OR MaNhaCungCap<>?)", phone, skip, skip)).intValue();
    }
    public void themNhaCungCap(Connection c, String code, String name, String phone, String address, String email, String tax) throws SQLException {
        capNhat(c, "INSERT INTO dbo.NhaCungCap(MaNhaCungCap,TenNhaCungCap,SoDienThoai,DiaChi,Email,MaSoThue,CongNo,TrangThai) VALUES(?,?,?,?,?,?,0,N'Hoạt động')", code, name, phone, address, email, tax);
    }
    public void capNhatNhaCungCap(Connection c, String code, String name, String phone, String address, String email, String tax) throws SQLException {
        capNhat(c, "UPDATE dbo.NhaCungCap SET TenNhaCungCap=?,SoDienThoai=?,DiaChi=?,Email=?,MaSoThue=? WHERE MaNhaCungCap=?", name, phone, address, email, tax, code);
    }
    public int demSanPhamTheoNhaCungCap(Connection c, String code) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.SanPham WHERE MaNhaCungCap=?", code)).intValue();
    }
    public int xoaNhaCungCap(Connection c, String code) throws SQLException {
        return capNhat(c, "DELETE FROM dbo.NhaCungCap WHERE MaNhaCungCap=?", code);
    }
    public BigDecimal tongCongNoNhaCungCap(String maNhaCungCap) throws SQLException {
        Object v = motGiaTri("SELECT CongNo FROM dbo.NhaCungCap WHERE MaNhaCungCap=?", maNhaCungCap);
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }
    public List<NhaCungCap> danhSachNhaCungCapThucThe() throws SQLException {
        List<NhaCungCap> ketQua = new ArrayList<>();
        for (Map<String, Object> d : danhSachNhaCungCap()) {
            NhaCungCap ncc = timNhaCungCapThucThe(String.valueOf(d.get("supplier_code")));
            if (ncc != null) ketQua.add(ncc);
        }
        return ketQua;
    }
    public List<NhaCungCap> danhSachNhaCungCapHoatDongThucThe() throws SQLException {
        List<NhaCungCap> ketQua = new ArrayList<>();
        for (NhaCungCap ncc : danhSachNhaCungCapThucThe()) if ("Hoạt động".equals(ncc.getTrangThai())) ketQua.add(ncc);
        return ketQua;
    }
    public List<NhaCungCap> timDanhSachNhaCungCapThucThe(String key) throws SQLException {
        List<NhaCungCap> ketQua = new ArrayList<>();
        for (Map<String, Object> d : timDanhSachNhaCungCap(key)) {
            NhaCungCap ncc = timNhaCungCapThucThe(String.valueOf(d.get("supplier_code")));
            if (ncc != null) ketQua.add(ncc);
        }
        return ketQua;
    }
}

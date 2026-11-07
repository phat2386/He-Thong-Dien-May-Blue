package service;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
/**
 * Ghi ảnh hóa đơn ra file PDF khổ A4 (tự chia nhiều trang nếu hóa đơn dài).
 * Lớp giao diện chỉ chụp nội dung hóa đơn thành ảnh rồi gọi lớp này để ghi file.
 */
public class XuatHoaDonPdfService {
    public void xuatAnhRaPdf(BufferedImage anh, File file) throws IOException {
        final double rongTrang = 595.0;
        final double caoTrang = 842.0;
        final double le = 28.0;
        final double rongNoiDung = rongTrang - le * 2;
        final double caoNoiDung = caoTrang - le * 2;
        double tyLe = rongNoiDung / anh.getWidth();
        int caoMoiTrangPx = Math.max(1, (int) Math.floor(caoNoiDung / tyLe));
        List<BufferedImage> cacTrang = new ArrayList<BufferedImage>();
        for (int y = 0; y < anh.getHeight(); y += caoMoiTrangPx) {
            int caoPhan = Math.min(caoMoiTrangPx, anh.getHeight() - y);
            BufferedImage trang = new BufferedImage(anh.getWidth(), caoPhan, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = trang.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, trang.getWidth(), trang.getHeight());
                g.drawImage(anh, 0, 0, anh.getWidth(), caoPhan, 0, y, anh.getWidth(), y + caoPhan, null);
            } finally {
                g.dispose();
            }
            cacTrang.add(trang);
        }
        ghiPdfAnhNhieuTrang(cacTrang, file, rongTrang, caoTrang, le, rongNoiDung);
    }
    private void ghiPdfAnhNhieuTrang(List<BufferedImage> cacTrang, File file, double rongTrang, double caoTrang,
            double le, double rongNoiDung) throws IOException {
        int soTrang = cacTrang.size();
        int tongObject = 2 + soTrang * 3;
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> viTri = new ArrayList<Integer>();
        viTri.add(0);
        ghiAscii(pdf, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        viTri.add(pdf.size());
        ghiObject(pdf, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < soTrang; i++) kids.append(3 + i * 3).append(" 0 R ");
        viTri.add(pdf.size());
        ghiObject(pdf, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + soTrang + " >>");
        for (int i = 0; i < soTrang; i++) {
            BufferedImage trang = cacTrang.get(i);
            int pageObj = 3 + i * 3;
            int imageObj = pageObj + 1;
            int contentObj = pageObj + 2;
            viTri.add(pdf.size());
            ghiObject(pdf, pageObj, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + dinhDangSo(rongTrang) + " "
                + dinhDangSo(caoTrang) + "] /Resources << /XObject << /Im0 " + imageObj
                + " 0 R >> >> /Contents " + contentObj + " 0 R >>");
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            ImageIO.write(trang, "jpg", jpeg);
            byte[] duLieuAnh = jpeg.toByteArray();
            viTri.add(pdf.size());
            ghiAscii(pdf, imageObj + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + trang.getWidth()
                + " /Height " + trang.getHeight() + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
                + duLieuAnh.length + " >>\nstream\n");
            pdf.write(duLieuAnh);
            ghiAscii(pdf, "\nendstream\nendobj\n");
            double caoHienThi = rongNoiDung * trang.getHeight() / trang.getWidth();
            String lenh = "q " + dinhDangSo(rongNoiDung) + " 0 0 " + dinhDangSo(caoHienThi) + " "
                + dinhDangSo(le) + " " + dinhDangSo(caoTrang - le - caoHienThi) + " cm /Im0 Do Q";
            byte[] duLieuLenh = lenh.getBytes(StandardCharsets.US_ASCII);
            viTri.add(pdf.size());
            ghiAscii(pdf, contentObj + " 0 obj\n<< /Length " + duLieuLenh.length + " >>\nstream\n");
            pdf.write(duLieuLenh);
            ghiAscii(pdf, "\nendstream\nendobj\n");
        }
        int batDauXref = pdf.size();
        ghiAscii(pdf, "xref\n0 " + (tongObject + 1) + "\n");
        ghiAscii(pdf, "0000000000 65535 f \n");
        for (int i = 1; i <= tongObject; i++) {
            ghiAscii(pdf, String.format(Locale.ROOT, "%010d 00000 n \n", viTri.get(i)));
        }
        ghiAscii(pdf, "trailer\n<< /Size " + (tongObject + 1) + " /Root 1 0 R >>\nstartxref\n"
            + batDauXref + "\n%%EOF");
        try (FileOutputStream out = new FileOutputStream(file)) {
            pdf.writeTo(out);
        }
    }
    private void ghiObject(ByteArrayOutputStream out, int soObject, String noiDung) throws IOException {
        ghiAscii(out, soObject + " 0 obj\n" + noiDung + "\nendobj\n");
    }
    private void ghiAscii(ByteArrayOutputStream out, String noiDung) throws IOException {
        out.write(noiDung.getBytes(StandardCharsets.ISO_8859_1));
    }
    private String dinhDangSo(double so) {
        return String.format(Locale.ROOT, "%.2f", so);
    }
}

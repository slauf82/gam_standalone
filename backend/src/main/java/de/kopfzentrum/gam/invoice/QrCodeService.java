package de.kopfzentrum.gam.invoice;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class QrCodeService {
  public byte[] png(String text, int size) {
    try {
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size,
        Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(), EncodeHintType.MARGIN, 1));
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("QR-Code konnte nicht erzeugt werden", e);
    }
  }
}

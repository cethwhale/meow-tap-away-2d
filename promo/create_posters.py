from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).parent
FONT = Path(r"C:\Windows\Fonts\segoeui.ttf")
BOLD = Path(r"C:\Windows\Fonts\segoeuib.ttf")

def font(size, bold=False):
    return ImageFont.truetype(str(BOLD if bold else FONT), size)

def rr(draw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)

def text(draw, xy, value, size, color, bold=False, anchor=None):
    draw.text(xy, value, font=font(size, bold), fill=color, anchor=anchor)

def step(draw, y, number, title, body, width=500):
    rr(draw, (55, y, 55 + width, y + 125), 28, (255, 255, 255, 238), (211, 231, 247, 255), 2)
    draw.ellipse((76, y + 27, 132, y + 83), fill=(41, 152, 238, 255))
    text(draw, (104, y + 55), str(number), 27, "white", True, "mm")
    text(draw, (153, y + 24), title, 27, "#1D2747", True)
    text(draw, (153, y + 66), body, 20, "#657087")

def poster_install(background):
    im = Image.open(background).convert("RGBA")
    d = ImageDraw.Draw(im, "RGBA")
    rr(d, (42, 44, 596, 264), 35, (255, 255, 255, 240), (204, 231, 251, 255), 3)
    text(d, (324, 92), "MEOW TAP AWAY 2D", 25, "#2998EE", True, "mm")
    text(d, (70, 118), "CÀI ĐẶT &", 49, "#1D2747", True)
    text(d, (70, 171), "CHỌN ĐỘ KHÓ", 49, "#1D2747", True)

    step(d, 300, 1, "TẢI FILE APK", "Mở file vừa tải trên điện thoại")
    step(d, 445, 2, "CHO PHÉP CÀI ĐẶT", "Bật “Ứng dụng không xác định”")
    step(d, 590, 3, "MỞ GAME", "Chọn độ khó rồi bấm Chơi mới")

    rr(d, (55, 755, 310, 905), 30, (54, 199, 154, 245))
    text(d, (182, 792), "DỄ", 34, "white", True, "mm")
    text(d, (182, 840), "Tăng dần độ khó\nNhiều tim hơn", 20, "white", False, "mm")
    rr(d, (330, 755, 585, 905), 30, (255, 89, 103, 245))
    text(d, (457, 792), "KHÓ", 34, "white", True, "mm")
    text(d, (457, 840), "Gần 30 mũi tên\nChỉ có 1 tim", 20, "white", False, "mm")

    rr(d, (55, 935, 585, 1025), 24, (29, 39, 71, 232))
    text(d, (320, 980), "“Chơi tiếp” tự động lưu tiến độ", 22, "white", True, "mm")
    im.convert("RGB").save(ROOT / "01-cai-dat-va-chon-do-kho.jpg", quality=95, subsampling=0)

def poster_play(background):
    im = Image.open(background).convert("RGBA")
    d = ImageDraw.Draw(im, "RGBA")
    rr(d, (42, 45, 605, 255), 35, (255, 255, 255, 242), (204, 231, 251, 255), 3)
    text(d, (324, 92), "MEOW TAP AWAY 2D", 25, "#2998EE", True, "mm")
    text(d, (70, 120), "CÁCH CHƠI", 55, "#1D2747", True)
    text(d, (70, 186), "Gỡ đúng đường — giữ trọn tim!", 23, "#657087")

    step(d, 290, 1, "CHỌN ĐƯỜNG THOÁNG", "Chạm vào đường không bị chắn", 470)
    step(d, 435, 2, "NHÌN ĐẦU MŨI TÊN", "Đường sẽ trườn theo hướng đó", 470)
    step(d, 580, 3, "TRÁNH VA CHẠM", "Bấm đường bị chặn sẽ mất 1 tim", 470)

    rr(d, (55, 755, 530, 900), 28, (255, 255, 255, 242), (255, 89, 103, 255), 3)
    text(d, (82, 782), "GỢI Ý", 29, "#FF3B4E", True)
    text(d, (82, 828), "Đường đúng sẽ sáng màu đỏ.", 21, "#49546D")
    text(d, (82, 861), "Không dùng gợi ý → nhận thêm 1 lượt.", 19, "#657087")

    rr(d, (55, 930, 530, 1035), 26, (29, 39, 71, 238))
    text(d, (292, 966), "MỤC TIÊU", 20, "#6FE0BF", True, "mm")
    text(d, (292, 1003), "Dọn sạch toàn bộ mũi tên!", 25, "white", True, "mm")
    im.convert("RGB").save(ROOT / "02-huong-dan-cach-choi.jpg", quality=95, subsampling=0)

poster_install(ROOT / "install-background.png")
poster_play(ROOT / "play-background.png")

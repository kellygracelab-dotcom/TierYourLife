import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

S = 4
W, H = 400, 600
out = Path(sys.argv[1])
out.mkdir(parents=True, exist_ok=True)


def canvas(bg_top, bg_bottom):
    img = Image.new("RGB", (W * S, H * S), bg_top)
    d = ImageDraw.Draw(img)
    for y in range(H * S):
        t = y / (H * S)
        c = tuple(round(a + (b - a) * t) for a, b in zip(bg_top, bg_bottom))
        d.line([(0, y), (W * S, y)], fill=c)
    return img, d


def p(*pts):
    return [(x * S, y * S) for x, y in pts]


def ell(d, cx, cy, rx, ry, fill, outline=None, width=0):
    d.ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], fill=fill, outline=outline, width=width * S)


def shadow(d, cx, cy, rx, color):
    ell(d, cx, cy, rx, rx * 0.16, color)


def leaf(d, x, y, length, angle, width, fill):
    a = math.radians(angle)
    tip = (x + length * math.cos(a), y + length * math.sin(a))
    nx, ny = -math.sin(a) * width, math.cos(a) * width
    mid = (x + length * 0.5 * math.cos(a), y + length * 0.5 * math.sin(a))
    d.polygon(p((x, y), (mid[0] + nx, mid[1] + ny), tip, (mid[0] - nx, mid[1] - ny)), fill=fill)


def save(img, name):
    img.resize((W, H), Image.LANCZOS).save(out / f"{name}.png", optimize=True)


def strawberry():
    img, d = canvas((255, 228, 225), (250, 200, 196))
    shadow(d, 200, 470, 110, (230, 170, 166))
    d.polygon(p((95, 250), (305, 250), (290, 330), (240, 420), (200, 460), (160, 420), (110, 330)), fill=(214, 40, 57))
    ell(d, 200, 255, 105, 55, (214, 40, 57))
    for i, (x, y) in enumerate([(150, 290), (200, 300), (250, 290), (130, 340), (180, 345), (225, 345), (270, 340),
                                (160, 390), (205, 395), (245, 385), (190, 435), (215, 430)]):
        ell(d, x, y, 5, 8, (255, 214, 102))
    for ang in (200, 240, 270, 300, 340):
        leaf(d, 200, 215, 70, ang, 18, (46, 160, 67))
    d.rectangle(p((195, 160), (205, 215)), fill=(46, 120, 50))
    save(img, "strawberry")


def watermelon():
    img, d = canvas((224, 247, 230), (196, 234, 206))
    shadow(d, 200, 455, 140, (170, 214, 182))
    d.pieslice([60 * S, 160 * S, 340 * S, 440 * S], 0, 180, fill=(46, 125, 50))
    d.pieslice([72 * S, 172 * S, 328 * S, 428 * S], 0, 180, fill=(230, 245, 200))
    d.pieslice([84 * S, 184 * S, 316 * S, 416 * S], 0, 180, fill=(239, 71, 88))
    for x, y in [(130, 330), (170, 360), (200, 325), (230, 360), (270, 330), (200, 385), (150, 305), (250, 305)]:
        ell(d, x, y, 6, 10, (40, 30, 30))
    save(img, "watermelon")


def mango():
    img, d = canvas((255, 243, 214), (252, 226, 170))
    shadow(d, 200, 460, 115, (232, 200, 140))
    ell(d, 200, 320, 115, 140, (255, 166, 43))
    ell(d, 170, 300, 70, 95, (255, 196, 61))
    ell(d, 150, 260, 22, 30, (255, 225, 140))
    d.rectangle(p((196, 165), (206, 190)), fill=(110, 76, 40))
    leaf(d, 205, 180, 90, -30, 20, (67, 160, 71))
    save(img, "mango")


def cherries():
    img, d = canvas((252, 228, 236), (246, 200, 216))
    shadow(d, 200, 460, 130, (226, 172, 190))
    d.line(p((150, 380), (210, 180)), fill=(98, 120, 40), width=8 * S)
    d.line(p((260, 390), (210, 180)), fill=(98, 120, 40), width=8 * S)
    leaf(d, 212, 182, 80, -20, 20, (67, 160, 71))
    for cx, cy in [(145, 390), (262, 400)]:
        ell(d, cx, cy, 62, 60, (176, 18, 46))
        ell(d, cx - 20, cy - 22, 14, 10, (230, 110, 130))
    save(img, "cherries")


def lemon():
    img, d = canvas((255, 250, 210), (250, 238, 160))
    shadow(d, 200, 450, 120, (230, 214, 130))
    d.polygon(p((60, 310), (95, 280), (305, 280), (340, 310), (305, 340), (95, 340)), fill=(253, 216, 53))
    ell(d, 200, 310, 125, 105, (253, 216, 53))
    ell(d, 165, 270, 35, 18, (255, 240, 150))
    leaf(d, 300, 240, 70, -40, 18, (67, 160, 71))
    save(img, "lemon")


def grapes():
    img, d = canvas((237, 231, 246), (215, 204, 236))
    shadow(d, 200, 470, 100, (190, 176, 214))
    rows = [(5, 230), (4, 280), (4, 330), (3, 375), (2, 415), (1, 450)]
    for n, y in rows:
        for i in range(n):
            x = 200 + (i - (n - 1) / 2) * 50
            ell(d, x, y, 28, 28, (106, 27, 154))
            ell(d, x - 9, y - 10, 7, 5, (171, 120, 210))
    d.rectangle(p((196, 160), (206, 205)), fill=(110, 76, 40))
    leaf(d, 206, 185, 90, -25, 26, (67, 160, 71))
    save(img, "grapes")


def kiwi():
    img, d = canvas((232, 245, 214), (206, 232, 176))
    shadow(d, 200, 460, 120, (180, 206, 150))
    ell(d, 200, 300, 130, 130, (121, 85, 72))
    ell(d, 200, 300, 118, 118, (124, 179, 66))
    ell(d, 200, 300, 45, 45, (230, 238, 200))
    for k in range(18):
        a = 2 * math.pi * k / 18
        ell(d, 200 + 70 * math.cos(a), 300 + 70 * math.sin(a), 5, 9, (40, 30, 25))
    save(img, "kiwi")


def orange():
    img, d = canvas((255, 236, 214), (252, 214, 170))
    shadow(d, 200, 460, 120, (232, 186, 140))
    ell(d, 200, 300, 130, 130, (245, 124, 0))
    ell(d, 200, 300, 118, 118, (255, 243, 224))
    ell(d, 200, 300, 110, 110, (255, 167, 38))
    for k in range(10):
        a = 2 * math.pi * k / 10
        d.line(p((200, 300), (200 + 110 * math.cos(a), 300 + 110 * math.sin(a))), fill=(255, 224, 178), width=5 * S)
    ell(d, 200, 300, 12, 12, (255, 224, 178))
    save(img, "orange")


def banana():
    img, d = canvas((255, 249, 219), (250, 236, 176))
    shadow(d, 200, 460, 130, (230, 214, 150))
    for k in range(0, 101):
        a = math.radians(25 + 130 * k / 100)
        r = 26 + 26 * math.sin(math.pi * k / 100)
        x, y = 200 + 150 * math.cos(a), 180 + 170 * math.sin(a)
        ell(d, x, y, r, r, (255, 213, 79))
    for k in range(10, 91):
        a = math.radians(25 + 130 * k / 100)
        x, y = 200 + 168 * math.cos(a), 180 + 188 * math.sin(a)
        ell(d, x, y, 6, 6, (240, 180, 40))
    a = math.radians(25)
    ell(d, 200 + 150 * math.cos(a) + 8, 180 + 150 * math.sin(a) - 22, 13, 13, (110, 76, 40))
    a = math.radians(155)
    ell(d, 200 + 150 * math.cos(a), 180 + 170 * math.sin(a), 9, 9, (90, 60, 30))
    save(img, "banana")


def peach():
    img, d = canvas((255, 232, 224), (250, 206, 190))
    shadow(d, 200, 460, 120, (228, 176, 160))
    ell(d, 200, 310, 130, 125, (255, 138, 101))
    ell(d, 240, 290, 80, 90, (255, 171, 145))
    d.arc([120 * S, 190 * S, 280 * S, 430 * S], 250, 320, fill=(230, 100, 70), width=5 * S)
    leaf(d, 205, 190, 85, -35, 22, (67, 160, 71))
    save(img, "peach")


def carrot():
    img, d = canvas((255, 238, 220), (250, 216, 184))
    shadow(d, 200, 480, 70, (230, 190, 150))
    d.polygon(p((140, 220), (260, 220), (205, 470), (195, 470)), fill=(245, 124, 0))
    ell(d, 200, 222, 60, 22, (245, 124, 0))
    for y in (270, 320, 370, 410):
        d.line(p((178 + (y - 220) / 6, y), (200, y + 4)), fill=(200, 90, 0), width=4 * S)
    for ang in (240, 260, 280, 300):
        leaf(d, 200, 210, 100, ang, 14, (56, 142, 60))
    save(img, "carrot")


def tomato():
    img, d = canvas((255, 226, 222), (248, 196, 190))
    shadow(d, 200, 455, 130, (226, 164, 158))
    ell(d, 200, 320, 140, 120, (229, 57, 53))
    ell(d, 150, 290, 30, 22, (239, 120, 110))
    for ang in (200, 250, 290, 340, 20, 160):
        leaf(d, 200, 210, 60, ang, 12, (56, 142, 60))
    d.rectangle(p((196, 180), (204, 210)), fill=(46, 110, 50))
    save(img, "tomato")


def eggplant():
    img, d = canvas((240, 230, 246), (222, 204, 234))
    shadow(d, 200, 475, 100, (196, 176, 214))
    ell(d, 200, 350, 105, 115, (94, 53, 127))
    ell(d, 200, 260, 65, 80, (94, 53, 127))
    ell(d, 170, 330, 20, 50, (140, 100, 170))
    d.polygon(p((140, 200), (260, 200), (240, 235), (200, 220), (160, 235)), fill=(85, 139, 47))
    d.rectangle(p((193, 150), (207, 205)), fill=(85, 139, 47))
    save(img, "eggplant")


def broccoli():
    img, d = canvas((230, 244, 226), (204, 230, 198))
    shadow(d, 200, 470, 90, (176, 206, 170))
    d.polygon(p((170, 470), (230, 470), (215, 330), (185, 330)), fill=(156, 204, 101))
    for x, y, r in [(130, 280, 55), (270, 280, 55), (200, 230, 65), (160, 330, 45), (240, 330, 45), (200, 300, 55)]:
        ell(d, x, y, r, r, (46, 125, 50))
        ell(d, x - r * 0.3, y - r * 0.3, r * 0.3, r * 0.3, (76, 160, 80))
    save(img, "broccoli")


def corn():
    img, d = canvas((255, 248, 220), (250, 234, 180))
    shadow(d, 200, 485, 80, (230, 210, 150))
    ell(d, 200, 310, 62, 165, (255, 202, 40))
    for row in range(14):
        y = 170 + row * 21
        half = 55 * math.sqrt(max(0.0, 1 - ((y - 310) / 165) ** 2))
        for col in range(-3, 4):
            x = 200 + col * 16
            if abs(x - 200) < half - 8:
                ell(d, x, y, 7, 9, (255, 224, 102))
    d.polygon(p((200, 480), (120, 300), (110, 200), (190, 440)), fill=(124, 179, 66))
    d.polygon(p((200, 480), (280, 300), (290, 200), (210, 440)), fill=(104, 159, 56))
    save(img, "corn")


def pepper():
    img, d = canvas((222, 244, 230), (198, 232, 208))
    shadow(d, 200, 465, 115, (170, 206, 180))
    ell(d, 160, 330, 65, 115, (67, 160, 71))
    ell(d, 240, 330, 65, 115, (56, 142, 60))
    ell(d, 200, 340, 55, 120, (76, 175, 80))
    ell(d, 175, 280, 14, 40, (150, 210, 150))
    d.rectangle(p((193, 175), (207, 225)), fill=(56, 120, 50))
    ell(d, 200, 225, 40, 14, (56, 120, 50))
    save(img, "pepper")


for draw in (strawberry, watermelon, mango, cherries, lemon, grapes, kiwi, orange, banana, peach,
             carrot, tomato, eggplant, broccoli, corn, pepper):
    draw()

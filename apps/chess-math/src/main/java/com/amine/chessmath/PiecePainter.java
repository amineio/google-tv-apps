package com.amine.chessmath;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
/** Original sculpted artwork with the original paths retained as a fallback. */
public final class PiecePainter {
    private static final String TYPES = "pnbrqk";
    private static final float[] HEIGHTS = {.66f, .78f, .80f, .72f, .84f, .86f};
    private static Atlas sharedAtlas;
    private static boolean atlasLoaded;
    private final Atlas atlas;
    private final RectF destination = new RectF();
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public PiecePainter(Context context) {
        atlas = loadAtlas(context.getResources(), context.getPackageName());
    }

    private static synchronized Atlas loadAtlas(Resources resources, String packageName) {
        if (atlasLoaded) return sharedAtlas;
        atlasLoaded = true;
        int resource = resources.getIdentifier("chess_pieces_v4", "drawable", packageName);
        if (resource == 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resource, options);
        if (bitmap == null) return null;
        if (bitmap.getWidth() % 6 != 0 || bitmap.getHeight() % 2 != 0 || !bitmap.hasAlpha()) {
            bitmap.recycle();
            return null;
        }
        int width = bitmap.getWidth() / 6, height = bitmap.getHeight() / 2;
        Rect[] bounds = new Rect[12];
        int[] pixels = new int[width * height];
        for (int index = 0; index < bounds.length; index++) {
            int originX = index % 6 * width, originY = index / 6 * height;
            bitmap.getPixels(pixels, 0, width, originX, originY, width, height);
            int left = width, top = height, right = -1, bottom = -1;
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                if ((pixels[y * width + x] >>> 24) <= 8) continue;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
            // Reject an empty or edge-clipped tile; all pieces must be independently padded.
            if (right < left || left == 0 || top == 0 || right == width - 1 || bottom == height - 1) {
                bitmap.recycle();
                return null;
            }
            bounds[index] = new Rect(originX + left, originY + top,
                    originX + right + 1, originY + bottom + 1);
        }
        // Typical pieces are drawn below half their source size. Let hardware use
        // prefiltered mip levels when supported, without changing the source pixels.
        // This is a renderer hint; the source artwork still defines the contours.
        bitmap.setHasMipMap(true);
        sharedAtlas = new Atlas(bitmap, bounds);
        return sharedAtlas;
    }

    private static final class Atlas {
        final Bitmap bitmap;
        final Rect[] bounds;
        Atlas(Bitmap bitmap, Rect[] bounds) {
            this.bitmap = bitmap;
            this.bounds = bounds;
        }
    }

    private void place(Rect source, int type, float x, float y, float size) {
        float scale = Math.min(size * HEIGHTS[type] / source.height(), size * .76f / source.width());
        float width = source.width() * scale, height = source.height() * scale;
        float left = x + (size - width) / 2f;
        float bottom = y + size * .89f;
        destination.set(left, bottom - height, left + width, bottom);
    }

    private final Paint paint = new Paint(3);
    private final Path path = new Path();
    public void draw(Canvas canvas, char piece, float x, float y, float size) {
        if (piece == '.') return;
        int type = TYPES.indexOf(Character.toLowerCase(piece));
        if (type < 0) return;
        if (atlas != null) {
            Rect source = atlas.bounds[type + (Character.isUpperCase(piece) ? 0 : 6)];
            place(source, type, x, y, size);
            canvas.drawBitmap(atlas.bitmap, source, destination, bitmapPaint);
            return;
        }
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(size / 100f, size / 100f);
        boolean white = Character.isUpperCase(piece);
        int fill = white ? 0xfffff0cf : 0xff18251e;
        int edge = white ? 0xff263c2d : 0xffffedc9;
        path.reset();
        switch (Character.toLowerCase(piece)) {
            case 'p':                 path.moveTo(30,78);
            path.cubicTo(31,67,42,65,42,47);
            path.cubicTo(24,35,34,17,50,17);
            path.cubicTo(66,17,76,35,58,47);
            path.cubicTo(58,65,69,67,70,78);
            break;
            case 'r':                 path.moveTo(27,78);
            path.lineTo(36,42);
            path.lineTo(25,35);
            path.lineTo(25,17);
            path.lineTo(37,17);
            path.lineTo(37,27);
            path.lineTo(44,27);
            path.lineTo(44,17);
            path.lineTo(56,17);
            path.lineTo(56,27);
            path.lineTo(63,27);
            path.lineTo(63,17);
            path.lineTo(75,17);
            path.lineTo(75,35);
            path.lineTo(64,42);
            path.lineTo(73,78);
            break;
            case 'n':                 path.moveTo(27,78);
            path.cubicTo(35,63,50,57,54,43);
            path.lineTo(39,49);
            path.lineTo(24,43);
            path.lineTo(28,34);
            path.lineTo(41,24);
            path.lineTo(43,13);
            path.lineTo(53,20);
            path.cubicTo(78,24,80,54,72,78);
            break;
            case 'b':                 path.moveTo(28,78);
            path.cubicTo(35,63,42,66,42,53);
            path.cubicTo(18,40,42,20,50,14);
            path.cubicTo(58,20,82,40,58,53);
            path.cubicTo(58,66,65,63,72,78);
            break;
            case 'q':                 path.moveTo(29,78);
            path.lineTo(21,30);
            path.lineTo(37,43);
            path.lineTo(37,22);
            path.lineTo(50,40);
            path.lineTo(63,22);
            path.lineTo(63,43);
            path.lineTo(79,30);
            path.lineTo(71,78);
            break;
            case 'k':                 path.moveTo(29,78);
            path.lineTo(35,57);
            path.cubicTo(14,44,28,25,44,34);
            path.lineTo(44,25);
            path.lineTo(36,25);
            path.lineTo(36,15);
            path.lineTo(44,15);
            path.lineTo(44,7);
            path.lineTo(56,7);
            path.lineTo(56,15);
            path.lineTo(64,15);
            path.lineTo(64,25);
            path.lineTo(56,25);
            path.lineTo(56,34);
            path.cubicTo(72,25,86,44,65,57);
            path.lineTo(71,78);
            break;
            default: canvas.restore();
            return;
        }
        path.close();
        shape(canvas, path, fill, edge);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(23,77,77,89,4,4,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(edge);
        paint.setStrokeWidth(2.8f);
        canvas.drawRoundRect(23,77,77,89,4,4,paint);
        if (Character.toLowerCase(piece) == 'n') {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(edge);
            canvas.drawCircle(47,32,2,paint);
        }
        if (Character.toLowerCase(piece) == 'b') {
            paint.setColor(edge);
            paint.setStrokeWidth(3);
            canvas.drawLine(53,25,44,38,paint);
        }
        if (Character.toLowerCase(piece) == 'q') for(int i=0;i<5;i++) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            canvas.drawCircle(21+i*14.5f,i%2==0?27:19,4,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(edge);
            paint.setStrokeWidth(2);
            canvas.drawCircle(21+i*14.5f,i%2==0?27:19,4,paint);
        }
        canvas.restore();
    }
    private void shape(Canvas c, Path p, int fill, int edge) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        c.drawPath(p,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(2.8f);
        paint.setColor(edge);
        c.drawPath(p,paint);
    }
}

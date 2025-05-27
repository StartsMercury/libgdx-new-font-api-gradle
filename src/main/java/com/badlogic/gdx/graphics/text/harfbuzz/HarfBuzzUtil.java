package com.badlogic.gdx.graphics.text.harfbuzz;

import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.HarfBuzzHelper;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.StringBuilder;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.harfbuzz.HarfBuzz;
import org.lwjgl.util.harfbuzz.hb_feature_t;
import org.lwjgl.util.harfbuzz.hb_glyph_info_t;
import org.lwjgl.util.harfbuzz.hb_glyph_position_t;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.StringJoiner;

import static org.lwjgl.util.harfbuzz.HarfBuzz.HB_BUFFER_CONTENT_TYPE_GLYPHS;
import static org.lwjgl.util.harfbuzz.HarfBuzz.HB_BUFFER_CONTENT_TYPE_INVALID;
import static org.lwjgl.util.harfbuzz.HarfBuzz.HB_BUFFER_CONTENT_TYPE_UNICODE;

/**
 * Internal HarfBuzz bindings util. Prefer {@link org.lwjgl.util.harfbuzz.HarfBuzz}.
 * <p>
 * org.lwjgl.util.harfbuzz.HarfBuzz 1.8.1 bindings
 * <p>
 * Bindings are not exhaustive, as many functions are not meaningful in Java (such as {@code funcs} procedures),
 * or were not needed (possibly because I did not know what they do - org.lwjgl.util.harfbuzz.HarfBuzz documentation leaves a lot to be desired).
 * <p>
 * Both low-level API and Java object API is exposed here. Whole binding is structured so that it is easy to combine
 * both API levels, if needed. (Some functions are not exposed through high-level API and low-level API has greater overhead.)
 * <p>
 * There is no additional verification done for performance reasons.
 * <p>
 * Interesting links when digging in org.lwjgl.util.harfbuzz.HarfBuzz:
 * - https://mail.gnome.org/archives/gtk-i18n-list/2009-August/msg00025.html (and followups)
 * - https://chromium.googlesource.com/chromium/src/+/49cf5df2724445f3160b4fdf13a187295abe14fb/ui/gfx/render_text_harfbuzz.cc
 * - https://github.com/tangrams/harfbuzz-example/blob/master/src/hbshaper.h
 * - MAYBE https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6lcar.html ?
 * <p>
 * Some methods are documented in .h/.cc files, but not on the web.
 * It is expected that user will consult official org.lwjgl.util.harfbuzz.HarfBuzz documentation before using this binding.
 *
 * @author Jan Polák (Darkyenus)
 */
public final class HarfBuzzUtil {
	// NOTE might reintroduce entries, like the OOP-style methods if it is
	//      determined that this class may be used beyond just implementation.

    // @off
	/*JNI
	#include <harfbuzz/hb.h>
	#include <harfbuzz/hb-ft.h>

	// Test that the assumptions used in bridging types are valid

	// https://stackoverflow.com/a/19402196/2694196
	#define STATIC_ASSERT(test) typedef char static_assertion_helper[( !!(test) )*2-1 ]

	STATIC_ASSERT(sizeof(char) == sizeof(jbyte));
	STATIC_ASSERT(sizeof(uint16_t) == sizeof(jchar));

	STATIC_ASSERT(sizeof(hb_codepoint_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_mask_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_direction_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_position_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_tag_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_script_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(uint32_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(int) == sizeof(jint));
	STATIC_ASSERT(sizeof(unsigned int) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_buffer_content_type_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_buffer_flags_t) == sizeof(jint));
	STATIC_ASSERT(sizeof(hb_buffer_cluster_level_t) == sizeof(jint));

	STATIC_ASSERT(sizeof(hb_language_t) == sizeof(jlong));
	STATIC_ASSERT(sizeof(hb_buffer_t *) == sizeof(jlong));
	STATIC_ASSERT(sizeof(hb_glyph_info_t *) == sizeof(jlong));
	STATIC_ASSERT(sizeof(hb_face_t *) == sizeof(jlong));
	STATIC_ASSERT(sizeof(hb_font_t *) == sizeof(jlong));
	STATIC_ASSERT(sizeof(unsigned int *) == sizeof(jlong));
	STATIC_ASSERT(sizeof(hb_tag_t *) == sizeof(jlong));

	*/

	// region https://harfbuzz.github.io/harfbuzz-hb-common.html

	/**
	 * @param tag hb_tag_t
	 */
	public static String hb_tag_to_string(int tag) {
		char[] buf = new char[4];
		buf[0] = (char) ((tag >> 24) & 0xFF);
		buf[1] = (char) ((tag >> 16) & 0xFF);
		buf[2] = (char) ((tag >> 8) & 0xFF);
		buf[3] = (char) (tag & 0xFF);
		return new String(buf);
	}

	/**
	 * hb_direction_t
	 */
	public enum Direction {
		INVALID(0),
		LTR(4),
		RTL(5),
		TTB(6),
		BTT(7);

		public final int value;

		Direction(int value) {
			this.value = value;
		}

		public static Direction valueOf(int hb_direction_t) {
			switch (hb_direction_t) {
				case 4:
					return Direction.LTR;
				case 5:
					return Direction.RTL;
				case 6:
					return Direction.TTB;
				case 7:
					return Direction.BTT;
			}
			return Direction.INVALID;
		}
	}

	/**
	 * hb_script_t
	 */
	public static final class Script {

		public final int value;

		public Script(int value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return hb_tag_to_string(value);
		}
	}

	/**
	 * hb_language_t
	 */
	public static final class Language {

		public final long value;

		public Language(long value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return HarfBuzz.hb_language_to_string(value);
		}
	}

	//endregion

	//region https://harfbuzz.github.io/harfbuzz-Buffers.html

	/**
	 * hb_buffer_t
	 */
	public static final class Buffer extends Pointer {

		public Buffer(long addr) {
			super(addr);
		}

		public static Buffer create() {
			long ptr = HarfBuzz.hb_buffer_create();
			return new Buffer(ptr == 0 ? HarfBuzz.hb_buffer_get_empty() : ptr);
		}

		@Override
		public void destroy() {
			HarfBuzz.hb_buffer_destroy(addr);
		}

		public void reset() {
			HarfBuzz.hb_buffer_reset(addr);
		}


		public void add(String text, int textOffset, int textLength, int itemOffset, int itemLength) {
			hb_buffer_add_string(addr, text, textOffset, textLength, itemOffset, itemLength);
		}

		public void add(char[] text, int textOffset, int textLength, int itemOffset, int itemLength) {
			hb_buffer_add_utf16(addr, text, textOffset, textLength, itemOffset, itemLength);
		}

		/**
		 * @param buffer hb_buffer_t *
		 * @param text const uint16_t *
		 * @param text_offset offset into text
		 * @param text_length int
		 * @param item_offset unsigned int
		 * @param item_length int
		 */
		public static void hb_buffer_add_utf16 (long buffer, char[] text, int text_offset, int text_length, int item_offset, int item_length) {
			MemoryStack stack = MemoryStack.stackGet(); int stackPointer = stack.getPointer();
			try {
				ByteBuffer buf = stack.malloc(text_length * Character.BYTES);
				buf.asCharBuffer().put(text, text_offset, text_length).position(0);
				HarfBuzz.hb_buffer_add_utf16(buffer, buf, item_offset, item_length);
			} finally {
				stack.setPointer(stackPointer);
			}
		} /*
			hb_buffer_add_utf16((hb_buffer_t *) buffer, text + text_offset, text_length, item_offset, item_length);
		*/

		/**
		 * @see #hb_buffer_add_utf16(long, char[], int, int, int, int)
		 */
		public static void hb_buffer_add_string (long buffer, String text, int text_offset, int text_length, int item_offset, int item_length) {
			HarfBuzz.hb_buffer_add_utf16(buffer, text.subSequence(text_offset, text_length), item_offset, item_length);
		} /*MANUAL
			const jchar *text_chars = env->GetStringCritical(obj_text, 0);
			//TODO text_chars could be null when OOM, do we care? (applies to all array or string procedures)
			hb_buffer_add_utf16((hb_buffer_t *) buffer, (const uint16_t *) (text_chars + text_offset), text_length, item_offset, item_length);
			env->ReleaseStringCritical(obj_text, text_chars);
		*/

		public enum ContentType {
			INVALID(HB_BUFFER_CONTENT_TYPE_INVALID),
			UNICODE(HB_BUFFER_CONTENT_TYPE_UNICODE),
			GLYPHS(HB_BUFFER_CONTENT_TYPE_GLYPHS);

			public final int value;

			ContentType(int value) {
				this.value = value;
			}
		}

		public void setContentType(ContentType type) {
			HarfBuzz.hb_buffer_set_content_type(addr, type.value);
		}

		public ContentType getContentType() {
			final int contentType = HarfBuzz.hb_buffer_get_content_type(addr);
			switch (contentType) {
				case HB_BUFFER_CONTENT_TYPE_INVALID:
					return ContentType.INVALID;
				case HB_BUFFER_CONTENT_TYPE_UNICODE:
					return ContentType.UNICODE;
				case HB_BUFFER_CONTENT_TYPE_GLYPHS:
					return ContentType.GLYPHS;
				default:
					assert false : "Unrecognized content type: "+contentType;
					return ContentType.INVALID;
			}
		}

		public void setDirection(Direction direction) {
			HarfBuzz.hb_buffer_set_direction(addr, direction.value);
		}

		public Direction getDirection() {
			return Direction.valueOf(HarfBuzz.hb_buffer_get_direction(addr));
		}

		public Script getScript() {
			return new Script(HarfBuzz.hb_buffer_get_script(addr));
		}

		public Language getLanguage() {
			return new Language(HarfBuzz.hb_buffer_get_language(addr));
		}

		public void setFlags(int flags) {
			HarfBuzz.hb_buffer_set_flags(addr, flags);
		}

		/**
		 * hb_buffer_cluster_level_t
		 */
		public enum ClusterLevel {
			MONOTONE_GRAPHEMES(0),
			MONOTONE_CHARACTERS(1),
			CHARACTERS(2);

			public final int value;

			ClusterLevel(int value) {
				this.value = value;
			}

			public static ClusterLevel valueOf(int hb_buffer_cluster_level_t) {
				switch (hb_buffer_cluster_level_t) {
					case 0:
						return MONOTONE_GRAPHEMES;
					case 1:
						return MONOTONE_CHARACTERS;
					case 2:
						return CHARACTERS;
					default:
						return null;
				}
			}
		}

		public void setClusterLevel(ClusterLevel clusterLevel) {
			HarfBuzz.hb_buffer_set_cluster_level(addr, clusterLevel.value);
		}

		public ClusterLevel getClusterLevel() {
			return ClusterLevel.valueOf(HarfBuzz.hb_buffer_get_cluster_level(addr));
		}

		public int getLength() {
			return HarfBuzz.hb_buffer_get_length(addr);
		}

		public void setLength(int length) {
			HarfBuzz.hb_buffer_set_length(addr, length);
		}

		public void guessSegmentProperties() {
			HarfBuzz.hb_buffer_guess_segment_properties(addr);
		}

		/**
		 * @see #hb_buffer_get_glyph_infos_to(long, IntArray)
		 */
		public void getGlyphInfos(IntArray out) {
			hb_buffer_get_glyph_infos_to(addr, out);
		}

		/**
		 * Retrieves glyph infos in the buffer and stores it in out array.
		 * Can be called on both {@link HarfBuzz#HB_BUFFER_CONTENT_TYPE_GLYPHS} and {@link HarfBuzz#HB_BUFFER_CONTENT_TYPE_UNICODE} type buffer.
		 *
		 * After call, out/3 = amount of glyphs.
		 * Packing follows closely
		 * <a href="https://harfbuzz.github.io/harfbuzz-Buffers.html#hb-glyph-info-t-struct">hb_glyph_info_t struct</a>
		 * layout.
		 *
		 * <code>
		 * out[i/3 + 0] = either a Unicode code point (before shaping) or a glyph index (after shaping) of item i
		 * out[i/3 + 1] = after shaping contains hb_glyph_flags_t (and possibly other bits are set)
		 * out[i/3 + 2] = the index of the character in the original text, SEE hb_glyph_info_t!
		 * </code>
		 *
		 * @param buffer hb_buffer_t *
		 * @param out array to which result should be stored to. Any current contents are discarded.
		 */
		public static void hb_buffer_get_glyph_infos_to(long buffer, IntArray out) {
			hb_glyph_info_t.Buffer buf = HarfBuzz.hb_buffer_get_glyph_infos(buffer);
			if (buf == null) {
				out.clear();
				return;
			}

			int infoCount = buf.remaining();
			out.ensureCapacity(infoCount * 3);
			out.size = infoCount * 3;
			int[] arr = out.items;

			for (int g = 0, i = 0; g < infoCount; g++) {
				hb_glyph_info_t ptr = buf.get();

				arr[i++] = ptr.codepoint();
				arr[i++] = ptr.mask();
				arr[i++] = ptr.cluster();
			}

			{
				buf.position(0);
				StringBuilder sb = new StringBuilder(buf.remaining());
				while (buf.hasRemaining()) {
					sb.appendCodePoint(buf.get().codepoint());
				}
			}

			{
				buf.position(0);
				StringJoiner sb = new StringJoiner(" ");
				while (buf.hasRemaining()) {
					sb.add(Integer.toHexString(buf.get().codepoint()));
				}
			}
		}

		/**
		 * @see #hb_buffer_get_glyph_positions_to(long, IntArray)
		 */
		public void getGlyphPositions(IntArray out) {
			hb_buffer_get_glyph_positions_to(addr, out);
		}

		/**
		 * Retrieves shaped glyph positions and stores it in out array.
		 * Can be called only on {@link HarfBuzz#HB_BUFFER_CONTENT_TYPE_GLYPHS} (shaped) type buffer.
		 *
		 * After call, out/4 = amount of glyphs.
		 * Packing follows closely
		 * <a href="https://harfbuzz.github.io/harfbuzz-Buffers.html#hb-glyph-position-t-struct">hb_glyph_position_t struct</a>
		 * layout.
		 *
		 * <code>
		 * out[i/4 + 0] = x_advance - how much the line advances after drawing this glyph when setting text in horizontal direction
		 * out[i/4 + 1] = y_advance - how much the line advances after drawing this glyph when setting text in vertical direction
		 * out[i/4 + 2] = x_offset - how much the glyph moves on the X-axis before drawing it, this should not affect how much the line advances
		 * out[i/4 + 3] = y_offset - how much the glyph moves on the Y-axis before drawing it, this should not affect how much the line advances
		 * </code>
		 * All positions are relative to the current point.
		 *
		 * @param buffer hb_buffer_t *
		 * @param out array to which result should be stored to. Any current contents are discarded.
		 */
		public static void hb_buffer_get_glyph_positions_to(long buffer, IntArray out) {
			hb_glyph_position_t.Buffer buf = HarfBuzz.hb_buffer_get_glyph_positions(buffer);
            out.clear();
            if (buf == null) return;

            int infoCount = buf.remaining();
            out.ensureCapacity(infoCount * 4);
            out.size = infoCount * 4;
            int[] arr = out.items;

            for (int g = 0, i = 0; g < infoCount; g++) {
                hb_glyph_position_t ptr = buf.get();
                arr[i++] = ptr.x_advance();
                arr[i++] = ptr.y_advance();
                arr[i++] = ptr.x_offset();
                arr[i++] = ptr.y_offset();
            }
        }
	}

	//endregion

	//region https://harfbuzz.github.io/harfbuzz-hb-face.html
	// To be used with hb_ft faces.

	public static final class Face extends Pointer {

		public Face(long addr) {
			super(addr);
		}

		@Override
		public void destroy() {
			HarfBuzz.hb_face_destroy(addr);
		}

		//region https://harfbuzz.github.io/harfbuzz-hb-ft.html

		/**
		 * @see HarfBuzz#hb_ft_face_create_referenced(long)
		 */
		public static Face createReferenced(FreeType.Face face) {
			return new Face(HarfBuzz.hb_ft_face_create_referenced(HarfBuzzHelper.addressOf(face)));
		}

		//endregion

		public int getIndex() {
			return HarfBuzz.hb_face_get_index(addr);
		}

		/**
		 * @see #hb_face_get_table_tags(long, IntArray)
		 */
		public void getTableTags(IntArray out) {
			hb_face_get_table_tags(addr, out);
		}

		/**
		 * @param face hb_face_t *
		 * @param tags_out array of hb_tag_t
		 * @see HarfBuzz#hb_face_get_table_tags(long, int, IntBuffer, IntBuffer)
		 */
		public static void hb_face_get_table_tags (long face, IntArray tags_out) {
			tags_out.clear();
			try (MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer tableCount = stack.mallocInt(1);
				final int tagBatchCount = 3;
				IntBuffer tableTags = stack.mallocInt(tagBatchCount);
				int offset = 0;
				while (true) {
					tableCount.put(0, tagBatchCount);
					final int totalCount = HarfBuzz.hb_face_get_table_tags(face, offset, tableCount, tableTags);
					if (totalCount == 0) {
						return;
					}
					final int count = tableCount.get(0);
					if (count == 0) {
						break;
					}
					offset += count;
					for (int i = 0; i < count; i++) {
						tags_out.add(tableTags.get(i));
					}
				}
			}
		}

	}

	//endregion

	//region https://harfbuzz.github.io/harfbuzz-hb-font.html

	public static final class Font extends Pointer {

		public Font(long addr) {
			super(addr);
		}

		@Deprecated // TODO Does not seem to work
		public Font(Face face) {
			super(HarfBuzz.hb_font_create(face.addr));
		}

		@Override
		public void destroy() {
			HarfBuzz.hb_font_destroy(addr);
		}

		//region https://harfbuzz.github.io/harfbuzz-hb-ft.html

		/**
		 * @see HarfBuzz#hb_ft_font_create_referenced(long)
		 */
		public static Font createReferenced(FreeType.Face face) {
			return new Font(HarfBuzz.hb_ft_font_create_referenced(HarfBuzzHelper.addressOf(face)));
		}

		public static final int[] NO_FEATURES = new int[0];

		/**
		 * @see #hb_shape(long, long, int[])
		 */
		public void shape(Buffer buffer, int[] features) {
			Buffer.hb_buffer_get_glyph_infos_to(this.addr, new IntArray());
			hb_shape(addr, buffer.addr, features);
		}

		/**
		 * @param font hb_font_t *
		 * @param buffer hb_buffer_t *
		 * @param features const hb_feature_t * (+ num_features unsigned int)
		 *                 Contains features, concatenated. Size should thus be divisible by 4.
		 *                 Not null.
		 */
		public static void hb_shape (long font, long buffer, int[] features) {
			if (features == null) {
				HarfBuzz.hb_shape(font, buffer, null);
				return;
			}

			int featureCount = features.length / 4;
			MemoryStack stack = MemoryStack.stackGet(); int pointer = stack.getPointer();
			try {
				hb_feature_t.Buffer feats = hb_feature_t.calloc(featureCount, stack);
				for (int f = 0, i = 0; f < featureCount; f++) {
					feats
						.get()
						.tag(features[i++])
						.value(features[i++])
						.start(features[i++])
						.end(features[i++]);
				}
				HarfBuzz.hb_shape(font, buffer, feats);
			} finally {
				stack.setPointer(pointer);
			}
		} /*
			jsize featureCount = env->GetArrayLength(obj_features) / 4;
			hb_feature_t feats[featureCount];
			for (int f = 0, i = 0; f < featureCount; f++) {
				feats[f].tag = (hb_tag_t) features[i++];
				feats[f].value = (uint32_t) features[i++];
				feats[f].start = (unsigned int) features[i++];
				feats[f].end = (unsigned int) features[i++];
			}

			hb_shape((hb_font_t *) font, (hb_buffer_t *) buffer, feats, featureCount);
		*/

		/**
		 * @return const char **
		 */
		public static String[] hb_shape_list_shapers_strings() {
			PointerBuffer shapers = HarfBuzz.hb_shape_list_shapers();
			if (shapers == null) return new String[0];

			int shapersLen = 0;
			while (shapers.get() != 0L) {
				shapersLen++;
			}

			shapers.rewind();
			String[] result = new String[shapersLen];
			for (int i = 0; i < shapersLen; i++) {
				String str = shapers.getStringUTF8();
				result[i] = str;
			}

			return result;
		} /*
			// Zero terminated
			const char ** shapers = hb_shape_list_shapers();
			jsize shapersLen = 0;
			while (shapers[shapersLen] != 0) {
				shapersLen++;
			}

			jobjectArray result = env->NewObjectArray(shapersLen, env->FindClass("java/lang/String"), NULL);
			for (jsize i = 0; i < shapersLen; i++) {
				jstring str = env->NewStringUTF(shapers[i]);
				env->SetObjectArrayElement(result, i, str);
			}

			return result;
		*/

		//endregion

	}

	//endregion

	/**
	 * Those objects must be {@link #destroy()}ed when no longer used.
	 */
	public static abstract class Pointer implements Disposable {

		/**
		 * Address of the object in native code.
		 */
		public final long addr;

		Pointer(long addr) {
			assert addr != 0;
			this.addr = addr;
		}

		public abstract void destroy();

		/**
		 * Calls {@link #destroy()}.
		 */
		@Override
		public final void dispose() {
			destroy();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Pointer pointer = (Pointer) o;
			return addr == pointer.addr;
		}

		@Override
		public int hashCode() {
			final long addr = this.addr;
			return (int)(addr ^ (addr >>> 32));
			//return Long.hashCode(addr); -> inlined, as Java 8 only
		}

		@Override
		public String toString() {
			return getClass().getSimpleName()+"@"+addr;
		}
	}

	public static float toFloatFrom26p6(int fixedPoint26p6) {
		return fixedPoint26p6 / 64f;
	}

	public static long to26p6FromInt(int number) {
		return number * 64;
	}

}

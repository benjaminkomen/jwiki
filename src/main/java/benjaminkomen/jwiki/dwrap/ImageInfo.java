package benjaminkomen.jwiki.dwrap;

import lombok.Getter;
import okhttp3.HttpUrl;

import java.util.Comparator;
import java.util.Objects;

/**
 * Container object for a result returned by the ImageInfo MediaWiki module.
 *
 * @author Fastily
 */
@Getter
public final class ImageInfo extends DataEntry {
    /**
     * The image size (in bytes)
     */
    private int size;

    /**
     * The file's height (in pixels), if applicable.
     */
    private int height;

    /**
     * The file's width (in pixels), if applicable.
     */
    private int width;

    /**
     * The sha1 hash for this file
     */
    private String sha1;

    /**
     * The url of the full size image.
     */
    private HttpUrl url;

    /**
     * The MIME string of the file.
     */
    private String mime;

    /**
     * Constructor, creates an ImageInfo with all null fields.
     */
    protected ImageInfo() {

    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ImageInfo &&
                Objects.equals(size, ((ImageInfo) other).size) &&
                Objects.equals(height, ((ImageInfo) other).height) &&
                Objects.equals(width, ((ImageInfo) other).width) &&
                Objects.equals(sha1, ((ImageInfo) other).sha1) &&
                Objects.equals(url, ((ImageInfo) other).url) &&
                Objects.equals(mime, ((ImageInfo) other).mime) &&
                super.equals(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, height, width, sha1, url, mime) + super.hashCode();
    }

    @Override
    public String toString() {
        return "ImageInfo{" +
                "size=" + size +
                ", height=" + height +
                ", width=" + width +
                ", sha1='" + sha1 + '\'' +
                ", url=" + url +
                ", mime='" + mime + '\'' +
                '}';
    }

    public static Comparator<ImageInfo> comparator() {
        return Comparator.comparing(ImageInfo::getTimestamp);
    }
}
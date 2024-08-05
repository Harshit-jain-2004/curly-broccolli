package com.microsoft.notes.richtext.editor.styled

import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class HTMLImageElementMatcherTest {

    private val sampleSamsungHTMLwithNoImage = "<html><head>\n" +
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><meta content=\"text/html; charset=us-ascii\"></head><body><p dir=\"ltr\"><span style=\"font-size:30px\">23</span><br><span style=\"font-size:30px\">21</span><br><span style=\"font-size:30px\">22</span></p></body></html>"

    private val htmlWithTwoImages = "<html>\n" +
        "   <head>\n" +
        "      <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
        "      <meta content=\"text/html; charset=us-ascii\">\n" +
        "   </head>\n" +
        "   <body>\n" +
        "      <img data-outlook-trace=\"F:0|T:1\" src=\"https://picsum.photos/400/250\" style=\"height:100%; width:100%; object-fit:contain; display:block\">\n" +
        "\t  <img data-outlook-trace=\"F:0|T:1\" src=\"https://picsum.photos/400/250\" style=\"height:100%; width:100%; object-fit:contain; display:block\"> \n" +
        "      <p dir=\"ltr\"><span style=\"font-size:30px\">Abc</span></p>\n" +
        "   </body>\n" +
        "</html>"

    private val sampleSamsungHTMLwithMultipleImages = "<html><head>\n" +
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><meta content=\"text/html; charset=us-ascii\"></head><body><img src=\"cid:dca9fd62-b819-11eb-877c-739e76134a75(0_1080_1527)\" style=\"height:100%; width:100%; object-fit:contain; display:block\"><img src=\"cid:dcaa0d34-b819-11eb-aa8e-3f33398fbe5c(1_1080_1527)\" style=\"height:100%; width:100%; object-fit:contain; display:block\"><img src=\"cid:e39883aa-b819-11eb-9cb1-8faace8b1b02(2_1080_1527)\" style=\"height:100%; width:100%; object-fit:contain; display:block\"><img src=\"cid:0879850c-b81a-11eb-bcfa-ff6e46a72f1f(3_1080_1527)\" style=\"height:100%; width:100%; object-fit:contain; display:block\"><img src=\"cid:12cd329c-b81a-11eb-b25f-f79e8d72587e(4_1080_1527)\" style=\"height:100%; width:100%; object-fit:contain; display:block\"> <p dir=\"ltr\"><span style=\"font-size:11px\">Just got longer</span></p><p dir=\"ltr\"><span style=\"font-size:11px\">Page 1</span><br><br><br></p><p dir=\"ltr\"><span style=\"font-size:11px\">Ok ok</span></p></body></html>"

    @Test
    fun `test image src read correctly`() {
        val ranges = getImageSrcRangesSortedFromHtml(htmlWithTwoImages)
        assertThat(ranges.size, iz(2))

        assertThat(htmlWithTwoImages.substring(ranges[0].first, ranges[0].second), iz("https://picsum.photos/400/250"))
        assertThat(htmlWithTwoImages.substring(ranges[1].first, ranges[1].second), iz("https://picsum.photos/400/250"))
    }

    @Test
    fun `test html with no images`() {
        val ranges = getImageSrcRangesSortedFromHtml(sampleSamsungHTMLwithNoImage)
        assertThat(ranges.size, iz(0))
    }

    @Test
    fun `test html with multiple images`() {
        val ranges = getImageSrcRangesSortedFromHtml(sampleSamsungHTMLwithMultipleImages)
        assertThat(ranges.size, iz(5))
    }

    @Test
    fun `test html with extra spaces around src`() {
        val html = "<html>\n" +
            "   <body>\n" +
            "      <img data-outlook-trace=\"F:0|T:1\"   src   =    \"https://picsum.photos/400/250\"  style=\"height:100%; width:100%; object-fit:contain; display:block\">\n" +
            "   </body>\n" +
            "</html>"

        val ranges = getImageSrcRangesSortedFromHtml(html)
        assertThat(ranges.size, iz(1))
        assertThat(html.substring(ranges[0].first, ranges[0].second), iz("https://picsum.photos/400/250"))
    }

    @Test
    fun `test html with random attribute order`() {
        val html = "<html>\n" +
            "   <body>\n" +
            "      <img data-outlook-trace=\"F:0|T:1\" style=\"height:100%; width:100%; object-fit:contain; display:block\"  src=\"https://picsum.photos/400/250\">\n" +
            "   </body>\n" +
            "</html>"

        val ranges = getImageSrcRangesSortedFromHtml(html)
        assertThat(ranges.size, iz(1))
        assertThat(html.substring(ranges[0].first, ranges[0].second), iz("https://picsum.photos/400/250"))
    }

    @Test
    fun `test html with random new lines`() {
        val html = "<html>\n" +
            "   <body>\n" +
            "      <img data-outlook-trace=\"F:0|T:1\"\n" +
            "\n" +
            "\t  \n" +
            "\t\tsrc\n" +
            "\t\t\t=\n" +
            "\t\t\t\t\"https://picsum.photos/400/250\" \n" +
            "\t\tstyle=\"height:100%; width:100%; object-fit:contain; display:block\">\n" +
            "   </body>\n" +
            "</html>"

        val ranges = getImageSrcRangesSortedFromHtml(html)
        assertThat(ranges.size, iz(1))
        assertThat(html.substring(ranges[0].first, ranges[0].second), iz("https://picsum.photos/400/250"))
    }
}

package me.ienze.processing.flexagonCreator;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.dom.util.SAXDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.Arrays;

/**
 * @author Peter Bradshaw, Dominik Gmiterko
 */
public class FlexagonCreator {

    private static final String XLNS = "http://www.w3.org/1999/xlink";

    private double rowHeight = Math.sqrt(3) / 2;
    private double centerFromBase = 0.5 / Math.sqrt(3);
    private double centerFromTop = rowHeight - centerFromBase;

    private NodeList singleSheetTemplate;
    private NodeList layoutInstructions1Template;
    private NodeList layoutInstructions2Template;

    public FlexagonCreator() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/single-sheet.xml")) {
            singleSheetTemplate = createTemplate(is);
        }
        try (InputStream is = getClass().getResourceAsStream("/layout-instructions1.xml")) {
            layoutInstructions1Template = createTemplate(is);
        }
        try (InputStream is = getClass().getResourceAsStream("/layout-instructions2.xml")) {
            layoutInstructions2Template = createTemplate(is);
        }
    }

    public void createSvg(String[] imagePaths, OutputStream out) {

        if (imagePaths == null || imagePaths.length != 6) {
            throw new IllegalArgumentException("Requires 6 image paths.");
        }

        Document svg = SVGDOMImplementation.getDOMImplementation().createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);

        Element defs = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "defs");
        addClippingDefs(defs, svg);

        for (int i = 0; i < 6; i++) {
            addImage(defs, svg, imagePaths[i], "i" + i);
        }
        addImage(defs, svg, "http://www.flatfeetpete.com/flexagon/images/labels.jpg", "i6");

        Element svgRoot = svg.getDocumentElement();

        svgRoot.appendChild(defs);

        Element scaleGroup = trisToGroup(defs, singleSheetTemplate, svg);
        scaleGroup.setAttribute("transform", " scale(95) translate(-0.25,-" + (rowHeight * 1.5) + ") rotate(60,0," + (rowHeight * 2) + ") ");
        svgRoot.appendChild(scaleGroup);

        scaleGroup = trisToGroup(defs, layoutInstructions1Template, svg);
        scaleGroup.setAttribute("transform", "translate(360,0) scale(35)");
        svgRoot.appendChild(scaleGroup);

        scaleGroup = trisToGroup(defs, layoutInstructions2Template, svg);
        scaleGroup.setAttribute("transform", "translate(40,580) scale(35)");
        svgRoot.appendChild(scaleGroup);

        writeSVG(svg, out);
    }

    private void addClippingDefs(Element defs, Document svg) {
        Element clip = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "clipPath");
        clip.setAttribute("id", "clipup");
        Element path = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "path");
        path.setAttribute("d", "M 0.5 0 L 1 " + rowHeight + " L 0 " + rowHeight);
        clip.appendChild(path);
        defs.appendChild(clip);

        clip = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "clipPath");
        clip.setAttribute("id", "clipdown");
        path = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "path");
        path.setAttribute("d", "M 0.5 " + rowHeight + " L 0 0L 1 0");
        clip.appendChild(path);
        defs.appendChild(clip);
    }

    private void addImage(Element clippingDefs, Document svg, String imagePath, String id) {
        Element image = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "image");
        image.setAttributeNS(XLNS, "href", imagePath);
        image.setAttribute("id", id);
        image.setAttribute("width", "2");
        image.setAttribute("height", "" + (rowHeight * 2));
        image.setAttribute("preserveAspectRatio", "none");
        clippingDefs.appendChild(image);
    }

    protected Element trisToGroup(Element defs, NodeList template, Document svg) {
        Element scaleGroup = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "g");

        for (int i = 0; i < template.getLength(); i++) {
            Element t = (Element) template.item(i);
            String imageid = "i" + t.getAttribute("img");

            int section = Integer.parseInt(t.getAttribute("sec"), 10) - 1;
            int rotation = Integer.parseInt(t.getAttribute("rot"), 10);
            double xpos = Integer.parseInt(t.getAttribute("x"), 10) / 200.0;
            double ypos = Integer.parseInt(t.getAttribute("y"), 10) / (100.0 / rowHeight);
            boolean pointUp = (Integer.parseInt(t.getAttribute("pup")) != 0);

            Element localGroup = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "g");
            localGroup.setAttribute("transform", "translate(" + xpos + "," + ypos + ")");
            imageid = getImageSectionId(defs, svg, imageid, section, rotation);

            Element use = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "use");
            use.setAttributeNS(XLNS, "href", "#" + imageid);
            use.setAttribute("clip-path", "url(#" + (pointUp ? "clipup" : "clipdown") + ")");

            localGroup.appendChild(use);
            scaleGroup.appendChild(localGroup);
        }
        return scaleGroup;
    }

    protected String getImageSectionId(Element defs, Document svg, String imageid, int section, int rotation) {
        if ((section == 0) && (rotation == 0)) {
            return imageid;
        }
        String resultId = imageid + 'p' + section + 'r' + rotation;
        if (svg.getElementById(resultId) != null) {
            return resultId;
        }

        int angle = 60 * rotation;
        Element transImage = svg.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "use");
        String transarg = "";
        // This is total horseshit, should have had 0,0 center of triangle
        double rotateYcenter;
        if ((rotation & 1) == 1) {
            rotateYcenter = ((section & 1) == 1)
                    ? centerFromBase : centerFromTop;
            if ((section & 1) == 1) {
                transarg = "translate(0," + centerFromBase + ")";
            } else {
                transarg = "translate(0,-" + centerFromBase + ")";
            }
        } else {
            rotateYcenter = ((section & 1) == 1)
                    ? centerFromBase : centerFromTop;
        }

        transImage.setAttribute("transform", transarg +
                "rotate(" + angle + ",0.5," + rotateYcenter + ") " +
                "translate(-" +
                (section % 3) * 0.5
                + ",-" +
                Math.floor(section / 3) * rowHeight
                + ") ");
        transImage.setAttributeNS(XLNS, "href", "#" + imageid);
        transImage.setAttribute("id", resultId);

        defs.appendChild(transImage);

        return resultId;
    }

    private NodeList createTemplate(InputStream in) throws IOException {
        Document document = createSVGDocument(in);
        return document.getDocumentElement().getElementsByTagName("Triangle");
    }

    private Document createSVGDocument(InputStream in) throws IOException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXDocumentFactory factory = new SAXDocumentFactory(GenericDOMImplementation.getDOMImplementation(), parser);
        return factory.createDocument(null, in);
    }

    private void writeSVG(Document doc, OutputStream out) {

        try {
            //Determine output type:
            SVGTranscoder t = new SVGTranscoder();

            //Set transcoder input/output
            TranscoderInput input = new TranscoderInput(doc);
            OutputStreamWriter ostream = new OutputStreamWriter(out);
            TranscoderOutput output = new TranscoderOutput(ostream);

            //Perform transcoding
            t.transcode(input, output);
            ostream.flush();
            ostream.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TranscoderException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 7) {
            System.err.println("Incorrect number of arguments: <output path> <image path>{6}");
            return;
        }

        try (FileOutputStream out = new FileOutputStream(args[0])) {

            FlexagonCreator flexagonCreator = new FlexagonCreator();

            flexagonCreator.createSvg(Arrays.copyOfRange(args, 1, 7), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

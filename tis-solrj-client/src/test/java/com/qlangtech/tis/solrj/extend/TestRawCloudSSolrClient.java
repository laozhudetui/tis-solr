/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.qlangtech.tis.solrj.extend;

import junit.framework.TestCase;
import org.apache.solr.client.solrj.FastStreamingDocsCallback;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.JavabinTupleStreamParser;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.DataEntry;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月17日
 */
public class TestRawCloudSSolrClient extends TestCase {

    public void testStreamingBinaryResponseParser() throws Exception {
        byte b = 36;
        System.out.println("(b & 0x80):" + (b & 0x80));

        Object a = 0x3FF;

        System.out.println(a);

        byte tagByte = 33 >>> 5;

        System.out.println((int) tagByte);
        b = -22;
        System.out.println("-22 >>> 1," + (b));
        System.out.println("-22 >>> 1," + (b >> 1));

        FastStreamingDocsCallback callback = new FastStreamingDocsCallback() {
            @Override
            public Object initDocList(Long numFound, Long start, Float maxScore) {
                System.out.println("numFound:" + numFound);
                return new int[1];
            }

            @Override
            public Object startDoc(Object docListObj) {
                return new Tuple();
            }

            @Override
            public void field(DataEntry field, Object docObj) {
                Tuple pojo = (Tuple) docObj;
                pojo.put(field.name(), field.val());
//                if ("emp_no".equals(field.name())) {
//                    pojo.empNo = field.val();
//                }
            }

            @Override
            public void endDoc(Object docObj) {
                Tuple pojo = (Tuple) docObj;

                System.out.println(pojo.getFields().entrySet().stream().map((e) -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(",")));

            }
        };
        // String binFile = "javabin_sample.bin";
        // String binFile = "search4employee2_export.bin";
        String binFile = "search4employee2_export_all.bin";
        SolrDocumentList list = null;
        Map<String, Object> next = null;

//        try (InputStream input = (TestRawCloudSSolrClient.class.getResourceAsStream(binFile))) {
//            byte[] bytes = IOUtils.toByteArray(input);
//            System.out.println("bytes.length:" + bytes.length);
//            for (int i = 0; i < bytes.length; i++) {
//                System.out.print(Integer.toHexString(bytes[i]));
//                System.out.print(" ");
//            }
//
//
//        }

        int count = 0;
        long current = System.currentTimeMillis();
        try (InputStream input = (TestRawCloudSSolrClient.class.getResourceAsStream(binFile))) {
            assertNotNull(input);
            try (JavabinTupleStreamParser jbc = new JavabinTupleStreamParser(input, true)) {
                while ((next = jbc.next()) != null) {
                    System.out.println("============:" + next);
                    count++;
                }

//                @SuppressWarnings({"rawtypes"})
//                Object o =  jbc.unmarshal(input);
//                System.out.println(o);
                //list = (SolrDocumentList) o.get("response");
            }

//            try (JavaBinCodec jbc = new JavaBinCodec()) {
//                @SuppressWarnings({"rawtypes"})
//                Object o =  jbc.unmarshal(input);
//                System.out.println(o);
//                //list = (SolrDocumentList) o.get("response");
//            }
        }
        System.out.println("allcount:" + count + ",comsume:" + (System.currentTimeMillis() - current) + "ms");


//        StreamingBinaryResponseParser parser = new StreamingBinaryResponseParser(callback);
////        try (FastInputStream input = new FastInputStream(TestRawCloudSSolrClient.class.getResourceAsStream("search4employee2_export.bin")  )) {
////            parser.processResponse(input, null);
////        }
//        //try (FastInputStream input = new FastInputStream(TestRawCloudSSolrClient.class.getResourceAsStream("javabin_sample.bin"))) {
//        try (InputStream input = (TestRawCloudSSolrClient.class.getResourceAsStream(binFile))) {
//            assertNotNull(input);
//            parser.processResponse(input, TisUTF8.getName());
//        }

    }

//    public void testStreamIterator() throws Exception {
//        URL url = new URL("http://192.168.28.200:8080/solr/search4employee2/export?fl=emp_no&q=*:*&sort=emp_no%20asc");
//        HttpUtils.get(url, new ConfigFileContext.StreamProcess<Void>() {
//            @Override
//            public List<ConfigFileContext.Header> getHeaders() {
//                return PostFormStreamProcess.ContentType.Application_x_www_form_urlencoded.getHeaders();
//            }
//
//            @Override
//            public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//                try {
////                    byte[] bytes = IOUtils.toByteArray(stream);
////                    System.out.println("bytes.length:" + bytes.length);
//
//                    LineIterator lineIterator = IOUtils.lineIterator(stream, TisUTF8.get());
//                    while (lineIterator.hasNext()) {
//                        System.out.println(lineIterator.nextLine());
//                    }
//                    //System.out.println(IOUtils.toString(stream, TisUTF8.get()));
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//
//                return null;
//            }
//        });
//    }
}

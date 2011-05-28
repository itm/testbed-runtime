///**********************************************************************************************************************
// * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
// * All rights reserved.                                                                                               *
// *                                                                                                                    *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
// * following conditions are met:                                                                                      *
// *                                                                                                                    *
// * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
// *   disclaimer.                                                                                                      *
// * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
// *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
// * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
// *   products derived from this software without specific prior written permission.                                   *
// *                                                                                                                    *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
// **********************************************************************************************************************/
//
//package de.uniluebeck.itm.tr.rs.persistence.test;
//
//import eu.wisebed.api.rs.ConfidentialReservationData;
//import eu.wisebed.api.rs.User;
//
//import javax.xml.datatype.DatatypeConfigurationException;
//import javax.xml.datatype.DatatypeFactory;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.*;
//
///**
// * Created by IntelliJ IDEA.
// * User: nrohwedder
// * Date: 07.05.2010
// * Time: 11:44:57
// * To change this template use File | Settings | File Templates.
// */
//public class Comparison {
//
//    //TODO performance-verbesserung
//    public static boolean equals(Object actualObject, Object compareObject) {
//        if (compare(actualObject, compareObject) == -1) return false;
//        return true;
//    }
//
//    //-1 checked and failed
//    //0 not checked
//    //1 checked and correct
//
//    private static int preComparison(Object actualObject, Object compareObject) {
//        int status = 0;
//        //if primitive and not equal
//        if ((status = compareIfPrimitive(actualObject, compareObject)) != 0) return status;
//        //if String
//        if ((status = compareIfString(actualObject, compareObject)) != 0) return status;
//        //if list
//        if ((status = compareIfList(actualObject, compareObject)) != 0) return status;
//        //if array
//        if ((status = compareIfArray(actualObject, compareObject)) != 0) return status;
//        //if set
//        if ((status = compareIfSet(actualObject, compareObject)) != 0) return status;
//        //if map
//        if ((status = compareIfMap(actualObject, compareObject)) != 0) return status;
//
//        return status;
//    }
//
//    public static int compare(Object actualObject, Object compareObject) {
//        int status = preComparison(actualObject, compareObject);
//        if (status != 0) {
//            return status;
//        }
//
//        Map<Object, Object> objectMap = new HashMap<Object, Object>();
//        objectMap.put(actualObject, compareObject);
//
//        Map<Object, Object> tempMap = new HashMap<Object, Object>();
//
//        for (Object object : objectMap.keySet()) {
//            //get all declared methods
//            Class clazz = object.getClass();
//            List<Method> declaredMethods = new LinkedList<Method>();
//            declaredMethods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
//            while (true) {
//                clazz = clazz.getSuperclass();
//                if (clazz.equals(Object.class)) break;
//                declaredMethods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
//            }
//            //getting getters and compare
//            for (Method method : declaredMethods) {
//                //getting getters
//                if (!method.getName().startsWith("get")) continue;
//
//                Object invokedActualObject = null;
//                Object invokedCompareObject = null;
//
//                //try invoking
//                try {
//                    invokedActualObject = method.invoke(object);
//                    invokedCompareObject = method.invoke(objectMap.get(object));
//                }
//                catch (InvocationTargetException e) {
//                    continue;
//                }
//                catch (IllegalArgumentException e) {
//                    continue;
//                }
//                catch (IllegalAccessException e) {
//                    continue;
//                }
//
//                //start comparison
//                //check if null
//                if (compareIfNull(invokedActualObject, invokedCompareObject)) continue;
//                //preComparison
//                status = preComparison(invokedActualObject, invokedCompareObject);
//                if (status == -1) return status;
//
//                tempMap.put(invokedActualObject, invokedCompareObject);
//            }
//            if (tempMap.isEmpty()) return status;
//
//            objectMap = tempMap;
//            tempMap = new HashMap<Object, Object>();
//        }
//
//        return 0;
//    }
//
//    private static boolean compareIfNull(Object actualObject, Object compareObject) {
//        if (actualObject == null && compareObject == null) return true;
//        return false;
//    }
//
//    private static int compareIfPrimitive(Object actualObject, Object compareObject) {
//        if (actualObject.getClass().isPrimitive()) {
//            if (actualObject == compareObject) return 1;
//            return -1;
//        }
//        return 0;
//    }
//
//    private static int compareIfString(Object actualObject, Object compareObject) {
//        if (actualObject instanceof String) {
//            if (actualObject.equals(compareObject)) return 1;
//            return -1;
//        }
//        return 0;
//    }
//
//    private static int compareIfArray(Object actualObject, Object compareObject) {
//        if (actualObject.getClass().isArray()) {
//            Object[] actualObjectAsArray = (Object[]) actualObject;
//            Object[] compareObjectAsArray = (Object[]) compareObject;
//
//            if (actualObjectAsArray.length != compareObjectAsArray.length) return -1;
//
//            for (int i = 0; i < actualObjectAsArray.length; i++) {
//                return compare(actualObjectAsArray[i], compareObjectAsArray[i]);
//            }
//        }
//        return 0;
//    }
//
//    private static int compareIfList(Object actualObject, Object compareObject) {
//        if (actualObject instanceof List) {
//            List actualObjectAsList = (List) actualObject;
//            List compareObjectAsList = (List) compareObject;
//
//            return compareIfArray(actualObjectAsList.toArray(), compareObjectAsList.toArray());
//        }
//        return 0;
//    }
//
//    private static int compareIfSet(Object actualObject, Object compareObject) {
//        if (actualObject instanceof Set) {
//            Set actualObjectAsSet = (Set) actualObject;
//            Set compareObjectAsSet = (Set) compareObject;
//
//            return compareIfArray(actualObjectAsSet.toArray(), compareObjectAsSet.toArray());
//        }
//        return 0;
//    }
//
//    private static int compareIfMap(Object actualObject, Object compareObject) {
//        if (actualObject instanceof Map) {
//            Map actualObjectAsMap = (Map) actualObject;
//            Map compareObjectAsMap = (Map) compareObject;
//
//            if (actualObjectAsMap.size() != compareObjectAsMap.size()) return -1;
//
//            //preComparison keys
//            if (compareIfSet(actualObjectAsMap.keySet(), compareObjectAsMap.keySet()) == -1) return -1;
//            //preComparison values
//            return (compareIfArray(actualObjectAsMap.values().toArray(), compareObjectAsMap.values().toArray()));
//        }
//        return 0;
//    }
//
//    //test
//
//   /* public static void main(String[] args) throws DatatypeConfigurationException {
//        List<User> users = new LinkedList<User>();
//        User user = new User();
//        user.setUrnPrefix("urn:wisebed:testbed1");
//        user.setUsername("Nils Rohwedder");
//        users.add(user);
//
//        List<String> urns = new LinkedList<String>();
//        urns.add("urn:wisebed:testbed1");
//
//        long millis = System.currentTimeMillis() + 200000;
//        GregorianCalendar gregorianCalendarFrom = new GregorianCalendar();
//        gregorianCalendarFrom.setTimeZone(TimeZone.getTimeZone("GMT+2"));
//        gregorianCalendarFrom.setTimeInMillis(millis);
//
//        GregorianCalendar gregorianCalendarTo = new GregorianCalendar();
//        gregorianCalendarTo.setTimeZone(TimeZone.getTimeZone("GMT+2"));
//        gregorianCalendarTo.setTimeInMillis(millis + 40);
//
//        ConfidentialReservationData data = new ConfidentialReservationData();
//        data.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarFrom));
//        data.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarTo));
//        data.setUsers(users);
//        data.setNodeURNs(urns);
//
//        ConfidentialReservationData otherData = new ConfidentialReservationData();
//        otherData.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarFrom));
//        otherData.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarTo));
//        otherData.setUsers(users);
//        otherData.setNodeURNs(urns);
//
//        System.out.println(equals(data, otherData));
//    }*/
//}
//

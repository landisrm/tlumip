/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.ts.odot;

 

import java.io.PrintWriter;

import java.io.FileWriter;

import java.io.IOException;


 

/**

 * Author: willison

 * Date: Sep 2, 2004

 * <p/>

 * Created by IntelliJ IDEA.

 */

public class HelloJava {

 

    public HelloJava(){}

 

    public static boolean write(String input){

        boolean success = false;

        try {

            PrintWriter writer = new PrintWriter(new FileWriter("/tmp/HelloJavaFromR.txt"));

            writer.println("This file was generated by calling Java from R");

            writer.println("Way to go, you did it");

            writer.println(input);

            writer.close();

            success = true;

        } catch (IOException e) {

            e.printStackTrace();

        }

        return success;

    }

 

    public static void main(String[] args) {

        HelloJava.write("Hi Jim");

    }

}
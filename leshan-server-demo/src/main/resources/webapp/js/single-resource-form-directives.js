/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

angular.module('singleResourceFormDirectives', [])

.directive('singleresourceform', function ($compile, $routeParams, $http, dialog,$filter) {
    return {
        restrict: "E",
        replace: true,
        scope: {
            resource: '=',
            parent: '='
        },
        templateUrl: "partials/single-resource-form.html",
        link: function (scope, element, attrs) {
            // define place holder
            if (scope.resource.def.type == "opaque") {
                scope.resource.placeholder = "byte value in Hexadecimal"; 
            } else if (scope.resource.def.type == "string") {
                scope.resource.placeholder = "string value";
            } else if (scope.resource.def.type == "float" || scope.resource.def.type == "integer") {
                scope.resource.placeholder = "number value";
            } else if (scope.resource.def.type == "time") {
                scope.resource.placeholder = "ISO 8601 time value (eg:2013-02-09T13:20+01:00)";
            } else if (scope.resource.def.type == "boolean") {
                scope.resource.placeholder = "true or false";
            }

            // define writable function
            scope.writable = function() {
                //if(scope.resource.def.instancetype != "multiple") {
                    if(scope.resource.def.hasOwnProperty("operations")) {
                        return scope.resource.def.operations.indexOf("W") != -1;
                    }
                //}
                return false;
            };

            function childFindById(p1, p2) {
                var result = null;
                for(var child=p1.firstChild; child!==null; child=child.nextSibling) {
                    if(child.id == p2) {
                        return child;
                    } else {
                        result = childFindById(child, p2);    
                    }
                    
                    if(result) {
                        break;
                    }
                }
                return result;
            }

            //add resource instance
            scope.add = function() {
                if(scope.resource.def.instancetype == "multiple") {
                    var obj = document.getElementById("resource-instance-list");
                    var array = document.getElementsByClassName("resource-instances");
                    var elements = array[0];
                    var cln = elements.cloneNode(true);
                    cln.setAttribute("id", "resource-instance" + array.length);

                    var input = childFindById(cln, "resource-input0");
                    input.setAttribute("id", "resource-input" + array.length);

                    var label = childFindById(cln, "resource-label0");
                    label.setAttribute("id", "resource-label" + array.length);
                    label.innerHTML = (label.innerHTML + " (" + array.length + ")");

                    obj.appendChild(cln);
                    return true;
                }
                return false;
            };

            // rm resource instance
            scope.rm = function() {
                if(scope.resource.def.instancetype == "multiple") {
                    var array = document.getElementsByClassName("resource-instances");
                    var elementId = "resource-instance" + (array.length - 1);
                    if(elementId != "resource-instance0") {
                        document.getElementById(elementId).remove();
                    }
                }
                return false;
            };

           // utility to get hex value from file
           function toByteArray(file, resolve, jsonObj, len) {
                var reader = new FileReader();
                reader.onload = function() {
                    var u = new Uint8Array(this.result),
                    a = new Array(u.length),
                    i = u.length;
                    while (i--) // map to hex
                        a[i] = (u[i] < 16 ? '0' : '') + u[i].toString(16);
                    u = null; // free memory

                    if(jsonObj != null && len != null) {
                        var i;
                        for(i = 0; i < len; i++) {
                            if(jsonObj[i] == "") {
                                jsonObj[i]= a.join('');
                            }
                        }
                        resolve(jsonObj);
                    } else {
                        resolve(a.join(''));
                    }
                  
                }
                reader.readAsArrayBuffer(file);
            }

            // Add promisedValue to get resource value
            scope.resource.getPromisedValue = function() {
                return new Promise(function(resolve, reject) {
                    if(scope.resource.def.instancetype == "multiple") {
                        var arr = document.getElementsByClassName("resource-input-class");
                        var jsonObj = {};
                        for(var item in arr) {
                            if(!isNaN(item)) {
                                var val = document.getElementById("resource-input"+ item).value;
                                if(val != "" || scope.resource.fileValue) {
                                    //if(scope.resource.def.type != 'opaque') {
                                        jsonObj[item] = val;
                                    //} else {
                                    //    jsonObj[item] = val.split('');
                                    //}

                                }
                            }
                        }

                        if (scope.resource.fileValue) {
                            toByteArray(scope.resource.fileValue, resolve, jsonObj, arr.length);
                        } else {                  
                            resolve(jsonObj);
                        }
                    } else {  
                        if (scope.resource.fileValue){
                            toByteArray(scope.resource.fileValue, resolve, null, null);
                        } else {                  
                            resolve(scope.resource.stringValue);
                        }
                    }
                });
            }
        }
    };
});
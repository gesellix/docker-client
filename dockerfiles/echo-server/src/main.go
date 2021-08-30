package main

import (
	"github.com/gin-gonic/gin"
	"io/ioutil"
	"log"
	"net/http"
)

func RootRequest(c *gin.Context) {
	c.String(http.StatusOK, "The wind caught it.")
}

func EchoRequest(c *gin.Context) {
	if c.Request.Body == nil {
		c.Data(http.StatusOK, c.Request.Header.Get("content-type"), nil)
	}
	requestBody, _ := ioutil.ReadAll(c.Request.Body)
	c.Data(http.StatusOK, c.Request.Header.Get("content-type"), requestBody)
}

func main() {
	r := gin.Default()
	r.GET("/", RootRequest)
	r.GET("/api/echo", EchoRequest)
	r.POST("/api/echo", EchoRequest)
	err := r.Run()
	if err != nil {
		log.Fatalf("Run failed: %s\n", err)
	}
}

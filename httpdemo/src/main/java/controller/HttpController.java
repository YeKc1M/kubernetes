package controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/http")
public class HttpController {

    @GetMapping
    public String index(){return "hello world!";}

    @GetMapping(value = "/list")
    public HttpEntity list(){
        try{
            RestTemplate template = new RestTemplate();
            ResponseEntity<HashMap> responseEntity = template.getForEntity("http://172.17.0.19:8080/all", HashMap.class);
            return responseEntity;
        }catch (Exception e){
            e.printStackTrace();
        }
        HashMap map = new HashMap();
        map.put("result", "invalid addr");
        return new HttpEntity<HashMap>(map);
    }
    //ip: <svc cluster ip>:8080
    @GetMapping(value = "/test")
    public HttpEntity test(@RequestParam(value = "ip") String ip, @RequestParam(value = "port", defaultValue = "8080") String port,
                    @RequestParam(value = "path", defaultValue = "") String path){
        RestTemplate template = new RestTemplate();
        ResponseEntity<HashMap> responseEntity = template.getForEntity("http://" + ip + ":" + port + "/" + path, HashMap.class);
        return responseEntity;
    }

    @GetMapping(value = "/test/map")
    public Map testResult(){
        Map<String, String> map = new HashMap<String, String>();
        map.put("result", "hello world!");
        return map;
    }

}

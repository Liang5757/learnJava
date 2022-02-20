<div class="animated fadeInRight">

    <div class="row">
        <div class="col-sm-12">
            <div class="ibox float-e-margins">
                <form id="commonForm" action="${base}/admin/course_arrange/listDialogWorkingTable.jhtml"  method="get">
                    <div class="ibox-content">

                        <div class="dataTables_wrapper form-inline dialog-table">
                            <table class="table table-striped table-bordered table-hover dataTables-example dataTable dialog-table">
                                <thead>
                                <tr>
                                    <th><input type="checkbox" class="i-checks" name="checkAll"></th>
                                    <th>序号</th>
                                    <th>作业表</th>
                                    <th>类型</th>
                                    <th>课程</th>
                                </tr>
                                </thead>
                                <tbody>
                                [#if page??]
                                  [#list page.content as workingTable]
                                  <tr>
                                      <td>
                                          <input type="checkbox" class="i-checks" name="ids" id="${workingTable.id}" value="${workingTable.id}" data-name="${workingTable.tableName}">
                                      </td>
                                      <td>${workingTable_index + 1}</td>
                                      <td>${workingTable.tableName}</td>
                                      <td>[#if workingTable.tableType==0]
  												                  作业题
  												                [/#if]
  												                [#if workingTable.tableType==1]
  												                  考试题
  												                [/#if]
  												                [#if workingTable.tableType==2]
  												                  资源表
  												                [/#if]
                                      </td>
                                      <td>${workingTable.course.courseName}</td>
                                  </tr>
                                  [/#list]
                                [/#if]
                                </tbody>
                            </table>
                            [@pagination pageNumber = page.pageNumber totalPages = page.totalPages]
                            [#include "/admin/include/paginationdialog.ftl"]
                            [/@pagination]
                        </div>
                    </div>
                </form>
                <div id="recordWorkingTableId" class="hidden">
                  [#list recordWorkingTableId as workingTableId]
                      <span>${workingTableId}</span>
                  [/#list]
                </div>
                <div id="recordWorkingTableName" class="hidden">
                  [#list recordWorkingTableName as workingTableName]
                      <span>${workingTableName}</span>
                  [/#list]
                </div>
            </div>
        </div>
    </div>
</div>

<script>

    var listTemplate = "${base}/admin/course_arrange/listDialogWorkingTable.jhtml";

    // 页码跳转
    $.pageSkipDialog = function (pageNumber) {
        $("#commonForm #pageNumber").val(pageNumber);
        reload();
    };

    $(document).on("click",".btn-search-fds",function(){
        $("#commonForm #pageNumber").val(1);
        reload();
    });
    function reload() {
        var index = layer.load(1, {
            shade: [0.1, '#fff'] //0.1透明度的白色背景
        });
        var data = $("#commonForm").serialize();
        $.get(listTemplate+"?courseId="+$("#courseId").val(), data, function (result, status) {
            layer.close(index);
            var tableCon = $("#selectWorkingTableList");
            tableCon.empty();
            tableCon.append(result);

           /* // 复选框
            $('.dialog-table .i-checks').iCheck({checkboxClass: 'icheckbox_square-red', radioClass: 'iradio_square-red'});
            // 选择所有，复选框改变
            $("#selectWorkingTableList input[name='checkAll']").on('ifChanged', function () {
                $("selectWorkingTableList input[name='ids']").iCheck($(this).is(":checked") == true ? 'check' : 'uncheck');
                if ($(this).is(":checked")) {
                    $.ajax({
                      type:"post",
                      url:"recordWorkingTable.jhtml",
                      data:{
                          flag:2
                      }
                    })
                } else {
                  $.ajax({
                    type:"post",
                    url:"recordWorkingTable.jhtml",
                    data:{
                        flag:3
                    }
                  })
                }
            });

            //选择单个,点击事件
            $("#selectWorkingTableList input[name='ids']").on('ifClicked', function () {
                //选中状态
                if ($(this).is(":checked") == false) {
                    $.ajax({
                      type:"post",
                      url:"recordWorkingTable.jhtml",
                      data:{
                          wrokingTableID:$(this).val(),
                          flag:1
                      }

                    })
                } else {
                  //取消选中
                  $.ajax({
                    type:"post",
                    url:"recordWorkingTable.jhtml",
                    data:{
                        wrokingTableID:$(this).val(),
                        flag:0
                    }
                  })
                }
            });

            //判断当前的复选框是否被选中，是则为选中状态
            var sp = $("#recordWorkingTableId").children()
            var $main_workingtable = $("#main_workingtable").text().split('、')
            for(var i = 0 ; i < $("#selectWorkingTableList input[name='ids']").length ; i++){
             for(var j = 0 ; j < sp.length;j++){
                  if($("#selectWorkingTableList input[name='ids']")[i].value == sp[j].innerHTML){
                      $("#"+sp[j].innerHTML).iCheck('check')
                  }
              }
              //获取原先的名称，使其为勾选状态
              for(var a = 0 ; a < $main_workingtable.length; a++){
                //getAttribute : 获取元素的属性值
                if($("#selectWorkingTableList input[name='ids']")[i].getAttribute("data-name") == $main_workingtable[a]){
                    $("#"+$("#selectWorkingTableList input[name='ids']")[i].getAttribute("id")).iCheck('check')
                  }
              }
            } */

           /* $("input[name='checkAll']").on('ifChanged', function () {
                    $("input[name='ids']").iCheck($(this).is(":checked") == true ? 'check' : 'uncheck');
                    if ($(this).is(":checked")) {
                        setCookie("workingtable_selectAll",1)  //设置cookie标志
                    } else {
                        if(getCookie("workingtable_selectAll")){  //反选则删除cookie
                            clearCookie("workingtable_selectAll")
                            clearCookie("workingtable_selectArr")
                        }
                    }
                });
                //选择单个,点击事件
                $("input[name='ids']").on('ifChanged', function () {
                    var arr = []
                    //选中状态
                    if ($(this).is(":checked")) {
                        if(getCookie("workingtable_selectArr")){
                            arr = getCookie("workingtable_selectArr").split(",")
                        }
                        if($.inArray($(this).val(),arr) == -1){  //去重
                            arr.push($(this).val())
                            console.log(arr)
                            setCookie("workingtable_selectArr",arr.join(","))
                        }
                        
                    } else {  //反选状态
                        if(getCookie("workingtable_selectArr")){
                            arr = getCookie("workingtable_selectArr").split(",")
                            if($.inArray($(this).val(),arr) != -1){
                                arr.splice($.inArray($(this).val(),arr),1)  //删除元素
                            }
                            setCookie("workingtable_selectArr",arr.join(","))

                            clearCookie("workingtable_selectAll")   //删除全选
                        }
                    }
                });
                //判断是否为全选 是则所有选择框为选中状态
                if(getCookie("workingtable_selectAll")){
                    $("input[name='checkAll']").iCheck("check")
                }
                //判断当前的复选框是否被选中，是则为选中状态
                if(getCookie("workingtable_selectArr")){
                    var sp = getCookie("workingtable_selectArr").split(",")
                    for(var i = 0 ; i < $("input[name='ids']").length ; i++){
                        for(var j = 0 ; j < sp.length;j++){
                            if($("input[name='ids']")[i].value == sp[j]){
                                $("#"+sp[j]).iCheck('check')
                            }
                        }
                    }
                } */
                /*
            var $main_workingtable = $("#main_workingtable").text().split('、')
            for(var i = 0 ; i < $("#selectWorkingTableList input[name='ids']").length ; i++){
              //获取原先的名称，使其为勾选状态
              for(var a = 0 ; a < $main_workingtable.length; a++){
                //getAttribute : 获取元素的属性值
                if($("#selectWorkingTableList input[name='ids']")[i].getAttribute("data-name") == $main_workingtable[a]){
                    $("#"+$("#selectWorkingTableList input[name='ids']")[i].getAttribute("id")).iCheck('check')
                  }
              }
            }*/


             // 初始化复选框
                $('.i-checks').iCheck({checkboxClass: 'icheckbox_square-red', radioClass: 'iradio_square-red',});

                // 选择所有，复选框改变
                $("input[name='checkAll']").on('ifClicked', function () {
                  //判断用户是否勾选了全选选项，是则将当前页面的复选框全部选中，并将其ID存入Cookie中
                  if ($(this).is(":checked")) {
                     $("input[name='ids']").iCheck('uncheck'); 
                  } else {
                    $("input[name='ids']").iCheck('check');   //用户取消全选，则将Cookie中存放的SelectArr数组清除
                  }
                });

  
                //选择单个,点击事件
                $("#selectWorkingTableList input[name='ids']").on('ifChanged', function () {
                      var arr = []
                      //选中状态
                    if ($(this).is(":checked")) {
                          //当Cookie中已经存有存放ID的数组，则在此基础上增加ID
                          if(getCookie("workingtable_selectArr")){
                            arr = getCookie("workingtable_selectArr").split(",")
                          }
                          var str = $(this).val()+":"+$(this).attr("data-name")
                          if($.inArray(str,arr) == -1){  //去重
                            arr.push(str)
                            setCookie("workingtable_selectArr",arr.join(","))
                          }
                          //用户勾选复选框时,判断当前页面所有复选框是否都已勾选中，是则将全选设为勾选状态
                          isSelectAll()
                    } else {  //反选状态
                        //查看Cookie中是否已存有用户选中的ID
                        if(getCookie("workingtable_selectArr")){
                          //取消勾选时，查看全选选项是否被选中，是则将其取消掉
                          if($("#selectWorkingTableList input[name='checkAll']").is(":checked")){
                            $("#selectWorkingTableList input[name='checkAll']").iCheck("uncheck")
                          }
                          //删除cookie中存在的此选项ID
                          arr = getCookie("workingtable_selectArr").split(",")
                          var str = $(this).val()+":"+$(this).attr("data-name")
                          if($.inArray(str,arr) != -1){   //查看数组中是否有存在此选项ID，有则删除此选项
                            arr.splice($.inArray(str,arr),1)  //删除元素
                          }
                          setCookie("workingtable_selectArr",arr.join(","))   //删除之后再重新设置Cookie
                        }   
                    }
                });

                //判断当前的复选框是否被选中，是则为选中状态
                if(getCookie("workingtable_selectArr")){
                    var sp = getCookie("workingtable_selectArr").split(",")
                    for(var i = 0 ; i < $("#selectWorkingTableList input[name='ids']").length ; i++){
                      for(var j = 0 ; j < sp.length;j++){
                        var id = sp[j].split(":")[0]
                        if($("#selectWorkingTableList input[name='ids']")[i].value == id){
                            $("#selectWorkingTableList #"+id).iCheck('check')
                        }
                      }
                    }
                    //获取cookie中的selectArr数组，遍历循环查看当前页面是否都被选中，是则将全选选项勾选中
                    isSelectAll()
                }
                // var sp = $("#recordWorkingTableId").children()
                var $main_workingtable = $("#main_workingtable").text().split("、")
                for(var i = 0 ; i < $("#selectWorkingTableList input[name='ids']").length ; i++){
                //获取原先的作业表名称，使其为勾选状态
                if($("#main_workingtable").children().length > 0){
                  $main_workingtable = $("#main_workingtable").children()
                  for(var a = 0 ; a < $main_workingtable.length; a++){
                    var main_workingtable_name = ""
                    //getAttribute : 获取元素的属性值
                    if($main_workingtable[a].innerHTML.indexOf("、") != -1){
                      main_workingtable_name = $main_workingtable[a].innerHTML.slice(0,$main_workingtable[a].innerHTML.indexOf("、"))
                    }else{
                      main_workingtable_name = $main_workingtable[a].innerHTML
                    }
                    if($("#selectWorkingTableList input[name='ids']")[i].getAttribute("data-name") == main_workingtable_name){
                        $("#"+$("#selectWorkingTableList input[name='ids']")[i].getAttribute("id")).iCheck('check')
                    }
                }
              }else {
                for(var a = 0 ; a < $main_workingtable.length; a++){
                  //getAttribute : 获取元素的属性值
                  if($("#selectWorkingTableList input[name='ids']")[i].getAttribute("data-name") == $main_workingtable[a]){
                      $("#"+$("#selectWorkingTableList input[name='ids']")[i].getAttribute("id")).iCheck('check')
                    }
                }
              }

             }
        }, "html");

    }

</script>
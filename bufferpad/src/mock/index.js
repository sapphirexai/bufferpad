const Mock = require('mockjs');
var Random = Mock.Random;
let data1 = Mock.mock({
    'Data|15': [ // 生成15条数据 数组
        {
            'MachineCode|+1': 200,
            'StartTime': '@DATETIME("yyyy-MM-dd HH:mm:ss")',
            'EndTime': '@DATETIME("yyyy-MM-dd HH:mm:ss")',
            'num|20-100': 20,
            'LotBatch|+1': 604000023402,
            'PartNum|+1': 'NYAS0' + 985,
            'BookBuildupStructure|1-10000': Random.integer(),
            'MachineStatus|1-9': 1,
            'PressProgram|+1': 'EMC' + 21,
            'Qty|100-1000':100, 
            'HeapNumber|1-9':1,
            'PanelQuan|100-1000':500,
            'BookNum|1-100':20


            // "shopId|+1": 1,//生成商品id，自增1
            // "shopMsg": "@ctitle(10)", //生成商品信息，长度为10个汉字
            // "shopName": "@cname",//生成商品名 ， 都是中国人的名字
            // "shopTel": /^1(5|3|7|8)[0-9]{9}$/,//生成随机电话号
            // "shopAddress": "@county(true)", //随机生成地址
            // "shopStar|1-5": "★", //随机生成1-5个星星
            // "salesVolume|30-1000": 30, //随机生成商品价格 在30-1000之间
            // "shopLogo": "@Image('100x40','#c33', '#ffffff','小北鼻')", //生成随机图片，大小/背景色/字体颜色/文字信息
            // "food|2": [ //每个商品中再随机生成2个food
            //     {
            //         "foodName": "@cname", //food的名字
            //         "foodPic": "@Image('100x40','#c33', '#ffffff','小可爱')",//生成随机图片，大小/背景色/字体颜色/文字信息
            //         "foodPrice|1-100": 20,//生成1-100的随机数
            //         "aname|2": [
            //             {
            //                 "aname": "@cname",
            //                 "aprice|30-60": 20
            //             }
            //         ]
            //     }
            // ]
        }
    ],
    "Success": true,
    "Message": null,
    "StatusCode": null,
    "ErrorCode": null
})


let data3 = Mock.mock({
//     'data1|1': [ // 生成15条数据 数组
//         {
//             'ID|15':[
//                 {'ID|+1': 200}
//             ],
//             'plan|15': [
//                 () => Random.integer(200, 1200)
//               ],
//               'actual|15': [
//                 () => Random.integer(100, 800)
//               ],

// }]
        "Data": {
            "name": [
                "101(A)",
                "104(A)",
                "201(A)",
                "302(A)",
                "301(SA)",
                "107",
                "202",
                "204",
                "206",
                "207",
                "208",
                "209",
                "307",
                "309",
                "310"
            ],
            "qty": [
                1512,
                1563,
                1482,
                1532,
                1521,
                522,
                0,
                0,
                515,
                497,
                499,
                532,
                538,
                0,
                523
            ],
            "target": [
                1666,
                1666,
                1666,
                1666,
                1666,
                542,
                542,
                542,
                542,
                542,
                542,
                542,
                542,
                542,
                542
            ]
        },
        "Success": true,
    "Message": null,
    "StatusCode": null,
    "ErrorCode": null
    })

let data2 = Mock.mock({
    
        "Data": {
            "categories": [
                "101(A)",
                "104(A)",
                "201(A)",
                "302(A)",
                "301(SA)",
                "107",
                "202",
                "204",
                "206",
                "207",
                "208",
                "209",
                "307",
                "309",
                "310"
            ],
            "series": [
                {
                    "name": "Running",
                    "color": "#87D300",
                    "data": [
                        82,
                        83,
                        78,
                        85,
                        79,
                        87,
                        0,
                        0,
                        86,
                        84,
                        88,
                        86,
                        89,
                        0,

                        85
                    ]
                },
                {
                    "name": "Book Block",
                    "color": "#005D26",
                    "data": [
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                    ]
                },
                {
                    "name": "Waiting Platen",
                    "color": "#D02090",
                    "data": [
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                    ]
                },
                {
                    "name": "Set up",
                    "color": "#9900FF",
                    "data": [
                        9,
                        8,
                        10,
                        8,
                        9,
                        8,
                        0,
                        0,
                        9,
                        9,
                        6,
                        6,
                        5,
                        0,
                        7


                    ]
                },
                {
                    "name": "Idle",
                    "color": "#fff",
                    "data": [
                        5,
                        3,
                        6,
                        2,
                        7,

                        2,
                        0,
                        0,
                        2,
                        2,
                        1,1,1,0,2

                        
                    ]
                },
                {
                    "name": "Malfunction",
                    "color": "red",
                    "data": [
                        2,
                        1,
                        2,
                        2,
                        3,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                    ]
                },
                {
                    "name": "No wip",
                    "color": "#51B2C5",
                    "data": [
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        100,
                        100,
                        0,
                        0,
                        0,
                        0,
                        0,
                        100,
                        0
                    ]
                },
                {
                    "name": "Maintenance",
                    "color": "#4876FF",
                    "data": [
                        2,
                        5,
                        4,
                        3,
                        2,
                        3,
                        0,
                        0,
                        3,
                        5,
                        5,
                        7,
                        5,
                        0,
                        6
                        

                    ]
                },
                {
                    "name": "Plan ShutDown",
                    "color": "#ffff00",
                    "data": [
                        2,
                        5,
                        4,
                        3,
                        2,
                        3,
                        0,
                        0,
                        3,
                        5,
                        5,
                        7,
                        5,
                        0,
                        6
                        

                    ]
                }
            ]
        },
        "Success": true,
        "message": null,
        "statusCode": null,
        "errorCode": null
    
})
Mock.mock(/oee\/Production/, 'get', () => { //三个参数。第一个：路径，第二个：请求方式post/get，第三个：回调，返回值
    return data1
})
Mock.mock(/oee\/oee/, 'get', () => { //三个参数。第一个：路径，第二个：请求方式post/get，第三个：回调，返回值
    return data2
})
Mock.mock(/oee\/Output/, 'get', () => { //三个参数。第一个：路径，第二个：请求方式post/get，第三个：回调，返回值
    return data3
})

